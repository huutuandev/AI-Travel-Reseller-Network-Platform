package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.request.merchant.UpdateMerchantBrandingRequest;
import com.aitravel.reseller.dto.request.merchant.UpdateMerchantProfileRequest;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantBrandingResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantProfileResponse;
import com.aitravel.reseller.service.merchant.MerchantProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Merchant profile & branding", description = "Tenant Admin business profile and White Label branding management APIs")
public class MerchantAccountController {

    private final MerchantProfileService profileService;

    @Operation(
            summary = "Get current merchant business profile",
            description = "Retrieves the authenticated merchant's business information, owner contact details, and White Label branding."
    )
    @GetMapping("/profile")
    public ApiResponse<MerchantProfileResponse> getMyProfile() {
        return ApiResponse.success("Merchant profile retrieved successfully", profileService.getMyProfile());
    }

    @Operation(
            summary = "Update current merchant business profile",
            description = "Updates business name, description, contact email, hotline phone, address, official website domain, and owner info."
    )
    @PutMapping("/profile")
    public ApiResponse<MerchantProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateMerchantProfileRequest request) {
        return ApiResponse.success("Merchant profile updated successfully", profileService.updateMyProfile(request));
    }

    @Operation(
            summary = "Get White Label branding configuration",
            description = "Retrieves the logo, favicon, primary/secondary colors, custom CSS, and footer text for the merchant's subdomain."
    )
    @GetMapping("/branding")
    public ApiResponse<MerchantBrandingResponse> getMyBranding() {
        return ApiResponse.success("Merchant branding retrieved successfully", profileService.getMyBranding());
    }

    @Operation(
            summary = "Update White Label branding configuration",
            description = "Configures White Label branding colors, logo/favicon URLs, custom CSS, and footer text for the merchant's workspace."
    )
    @PutMapping("/branding")
    public ApiResponse<MerchantBrandingResponse> updateMyBranding(
            @Valid @RequestBody UpdateMerchantBrandingRequest request) {
        return ApiResponse.success("Merchant branding updated successfully", profileService.updateMyBranding(request));
    }
}
