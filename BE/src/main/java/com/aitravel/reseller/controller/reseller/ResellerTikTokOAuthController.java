package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokConnectedAccountResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokOAuthUrlResponse;
import com.aitravel.reseller.service.reseller.TikTokOAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reseller/social-accounts/tiktok")
@RequiredArgsConstructor
@Tag(name = "Reseller social accounts")
public class ResellerTikTokOAuthController {
    private final TikTokOAuthService service;

    @GetMapping("/oauth-url")
    @PreAuthorize("hasRole('RESELLER')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a TikTok OAuth authorization URL")
    public ApiResponse<TikTokOAuthUrlResponse> authorizationUrl() {
        return ApiResponse.success("TikTok authorization URL created", service.createAuthorizationUrl());
    }

    @GetMapping("/callback")
    @Operation(summary = "Complete TikTok OAuth and link the account")
    public ApiResponse<TikTokConnectedAccountResponse> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        return ApiResponse.success("TikTok account connected", service.complete(code, state, error));
    }
}
