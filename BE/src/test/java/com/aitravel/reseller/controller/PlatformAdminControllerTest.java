package com.aitravel.reseller.controller;

import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.SecurityConfig;
import com.aitravel.reseller.security.jwt.*;
import com.aitravel.reseller.service.admin.AdminService;
import com.aitravel.reseller.controller.admin.PlatformAdminController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;

@WebMvcTest(PlatformAdminController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class PlatformAdminControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean
    AdminService service;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserRepository users;

    @Test void anonymousCannotReadAdminData() throws Exception {
        mvc.perform(get("/api/v1/admin/tenants")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles="RESELLER") void resellerCannotReadAdminData() throws Exception {
        mvc.perform(get("/api/v1/admin/tenants")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser(roles="ADMIN") void adminCanListTenants() throws Exception {
        when(service.tenants(0,10,"", "")).thenReturn(new AdminService.PageResult(List.of(),0,0,0,10));
        mvc.perform(get("/api/v1/admin/tenants")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test @WithMockUser(roles="ADMIN") void malformedIdIsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/admin/tenants/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles="ADMIN") void missingVersionIsBadRequest() throws Exception {
        mvc.perform(patch("/api/v1/admin/tenants/" + UUID.randomUUID() + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void suspendedAdminTokenIsDenied() throws Exception {
        UUID id = UUID.randomUUID();
        when(jwtUtil.validateToken("signed")).thenReturn(true);
        when(jwtUtil.getClaimsFromToken("signed")).thenReturn(io.jsonwebtoken.Jwts.claims().add("id", id.toString()).build());
        when(users.findById(id)).thenReturn(Optional.of(com.aitravel.reseller.entity.User.builder()
                .id(id).status("SUSPENDED").build()));
        mvc.perform(get("/api/v1/admin/tenants").header("Authorization","Bearer signed"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void revokedAdminRoleIsDeniedDespiteValidToken() throws Exception {
        UUID id = UUID.randomUUID();
        when(jwtUtil.validateToken("signed")).thenReturn(true);
        when(jwtUtil.getClaimsFromToken("signed")).thenReturn(io.jsonwebtoken.Jwts.claims().add("id", id.toString()).build());
        when(users.findById(id)).thenReturn(Optional.of(com.aitravel.reseller.entity.User.builder()
                .id(id).email("user@example.com").status("ACTIVE").roles(Set.of()).build()));
        mvc.perform(get("/api/v1/admin/tenants").header("Authorization","Bearer signed"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
