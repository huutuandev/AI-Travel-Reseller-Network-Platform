package com.aitravel.reseller.controller.admin;

import com.aitravel.reseller.dto.request.UpdateUserRoleRequest;
import com.aitravel.reseller.dto.request.UpdateUserStatusRequest;
import com.aitravel.reseller.dto.request.UserUpdateRequest;
import com.aitravel.reseller.dto.respone.PageResponse;
import com.aitravel.reseller.dto.respone.UserResponse;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.AdminUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private CustomUserDetails adminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        adminUser = new CustomUserDetails(UUID.randomUUID(), "admin@example.com", Collections.singletonList(new SimpleGrantedAuthority("ADMIN")));
        
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testGetUsers() throws Exception {
        UserResponse response = new UserResponse(UUID.randomUUID(), "user@example.com", "Test User", "1234567890", Set.of("USER"), "ACTIVE", Instant.now(), Instant.now());
        PageResponse<UserResponse> pageResponse = new PageResponse<>(Collections.singletonList(response), 0, 10, 1, 1, true);

        when(adminUserService.getUsers(any(), any(), any(), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users")
                .principal(SecurityContextHolder.getContext().getAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("user@example.com"));
    }

    @Test
    void testGetUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse response = new UserResponse(userId, "user@example.com", "Test User", "1234567890", Set.of("USER"), "ACTIVE", Instant.now(), Instant.now());

        when(adminUserService.getUserById(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"));
    }

    @Test
    void testUpdateUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserUpdateRequest request = new UserUpdateRequest("Updated Name", "0987654321");
        UserResponse response = new UserResponse(userId, "user@example.com", "Updated Name", "0987654321", Set.of("USER"), "ACTIVE", Instant.now(), Instant.now());

        when(adminUserService.updateUser(eq(userId), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/admin/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    @Test
    void testUpdateUserStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserStatusRequest request = new UpdateUserStatusRequest("INACTIVE");

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(SecurityContextHolder.getContext().getAuthentication())
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminUserService).updateUserStatus(eq(userId), any(UpdateUserStatusRequest.class), eq(adminUser.getId()));
    }

    @Test
    void testUpdateUserRole() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(Set.of("ADMIN"));

        mockMvc.perform(patch("/api/v1/admin/users/" + userId + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(SecurityContextHolder.getContext().getAuthentication())
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminUserService).updateUserRole(eq(userId), any(UpdateUserRoleRequest.class), eq(adminUser.getId()));
    }
}
