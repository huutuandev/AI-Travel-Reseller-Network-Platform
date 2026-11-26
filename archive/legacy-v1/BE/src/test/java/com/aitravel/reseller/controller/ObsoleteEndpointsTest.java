package com.aitravel.reseller.controller;

import com.aitravel.reseller.controller.merchant.ProductController;
import com.aitravel.reseller.security.SecurityConfig;
import com.aitravel.reseller.security.jwt.JwtAccessDeniedHandler;
import com.aitravel.reseller.security.jwt.JwtAuthenticationEntryPoint;
import com.aitravel.reseller.security.jwt.JwtAuthenticationFilter;
import com.aitravel.reseller.security.jwt.JwtUtil;
import com.aitravel.reseller.security.user.CustomUserDetailsService;
import com.aitravel.reseller.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aitravel.reseller.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ObsoleteEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("14a. Endpoint GET /api/v1/admin/products/pending không còn tồn tại (404 Not Found)")
    void testPendingProductsEndpoint_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/pending"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("14b. Endpoint PATCH /api/v1/admin/products/{id}/approve không còn tồn tại (404 Not Found)")
    void testApproveProductEndpoint_NotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/products/{id}/approve", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
