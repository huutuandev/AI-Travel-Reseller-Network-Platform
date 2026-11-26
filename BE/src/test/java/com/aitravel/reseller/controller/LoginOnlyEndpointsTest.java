package com.aitravel.reseller.controller;

import com.aitravel.reseller.dto.respone.AuthResponse;
import com.aitravel.reseller.exception.GlobalExceptionHandler;
import com.aitravel.reseller.security.SecurityConfig;
import com.aitravel.reseller.security.jwt.*;
import com.aitravel.reseller.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, AccountController.class}, properties = {
        "app.features.actor-functions=false", "app.features.account-management=false"})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class,
        JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class LoginOnlyEndpointsTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private com.aitravel.reseller.repository.UserRepository userRepository;

    @Test
    void loginStillIssuesTokens() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.builder()
                .accessToken("access").refreshToken("refresh").tokenType("Bearer").build());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"Password@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").value("access"));
        verify(authService).login(any());
    }

    @Test
    void accountManagementIsUnavailable() throws Exception {
        for (String endpoint : new String[]{"register", "verify-email", "resend-verification",
                "forgot-password", "verify-reset-otp", "reset-password"}) {
            mvc.perform(post("/api/v1/auth/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                    .content("{}")).andExpect(status().isNotFound());
        }
        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void actorEndpointsAreUnavailableEvenToAdmin() throws Exception {
        for (String endpoint : new String[]{"/api/v1/products", "/api/v1/merchant/me",
                "/api/v1/merchant/me/products"}) {
            mvc.perform(get(endpoint)).andExpect(status().isNotFound());
        }
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/merchant/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());

    }
}