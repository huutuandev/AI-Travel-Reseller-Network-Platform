package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.request.merchant.MerchantOnboardingCompleteRequest;
import com.aitravel.reseller.dto.request.merchant.MerchantOnboardingRegisterRequest;
import com.aitravel.reseller.dto.request.merchant.MerchantResendOtpRequest;
import com.aitravel.reseller.dto.request.merchant.MerchantVerifyOtpRequest;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantOnboardingCompleteResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantOtpVerifyResponse;
import com.aitravel.reseller.dto.respone.merchant.SubdomainCheckResponse;
import com.aitravel.reseller.service.merchant.MerchantOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/merchant/onboarding")
@RequiredArgsConstructor
@Tag(name = "Merchant onboarding", description = "3-step merchant registration, OTP verification, and workspace onboarding APIs")
public class MerchantOnboardingController {

    private final MerchantOnboardingService onboardingService;

    @Operation(
            summary = "Step 1: Submit initial merchant registration info and request OTP",
            description = "Accepts fullName (Your name), businessName (Business name), and email (Work email). Sends a 6-digit OTP to the work email."
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody MerchantOnboardingRegisterRequest request) {
        onboardingService.registerInitialInfo(request);
        return ResponseEntity.ok(ApiResponse.success("Verification OTP has been sent to your work email."));
    }

    @Operation(
            summary = "Resend verification OTP",
            description = "Resends a new 6-digit OTP to the work email if the onboarding session is still active."
    )
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody MerchantResendOtpRequest request) {
        onboardingService.resendOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("A new verification OTP has been sent to your work email."));
    }

    @Operation(
            summary = "Step 2: Verify email OTP",
            description = "Verifies the 6-digit OTP and returns a temporary onboardingToken along with a suggested subdomain for Step 3."
    )
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<MerchantOtpVerifyResponse>> verifyOtp(
            @Valid @RequestBody MerchantVerifyOtpRequest request) {
        MerchantOtpVerifyResponse response = onboardingService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. Please complete your business profile.", response));
    }

    @Operation(
            summary = "Check subdomain availability",
            description = "Checks whether a subdomain is valid and available for a new merchant workspace."
    )
    @GetMapping("/check-subdomain")
    public ResponseEntity<ApiResponse<SubdomainCheckResponse>> checkSubdomain(
            @RequestParam String subdomain) {
        SubdomainCheckResponse response = onboardingService.checkSubdomain(subdomain);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @Operation(
            summary = "Step 3: Complete merchant profile, domain & subdomain setup",
            description = "Creates the ACTIVE merchant user, ACTIVE tenant workspace, and default White Label branding, then returns JWT tokens."
    )
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<MerchantOnboardingCompleteResponse>> complete(
            @Valid @RequestBody MerchantOnboardingCompleteRequest request) {
        MerchantOnboardingCompleteResponse response = onboardingService.completeOnboarding(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Merchant account and workspace created successfully.", response));
    }
}
