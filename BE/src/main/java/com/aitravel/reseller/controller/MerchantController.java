package com.aitravel.reseller.controller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.MerchantResponse;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/dashboard")
@RequiredArgsConstructor
@Tag(name = "Merchant Dashboard", description = "Dashboard APIs for merchants")
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "Merchant dashboard summary",
            description = "Returns the number of CONFIRMED orders and total revenue of the logged-in merchant.",
            security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> getDashboard(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                ApiResponse.success("OK", merchantService.getDashboard(user.getId())));
    }
}