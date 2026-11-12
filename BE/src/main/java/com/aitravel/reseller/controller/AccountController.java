package com.aitravel.reseller.controller;

import com.aitravel.reseller.dto.request.*;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.features.account-management", havingValue = "true")
@Tag(name = "Account management", description = "Temporarily disabled registration and password recovery APIs")
public class AccountController {
    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new user account with INACTIVE status and sends a verification OTP to the email.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please check your email for the OTP."));
    }

    @Operation(summary = "Verify Email OTP", description = "Verifies the OTP sent to email to activate the user account.")
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. Your account is now active."));
    }

    @Operation(summary = "Resend Verification OTP", description = "Resends a new verification OTP to the given email if the account is not yet active.")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("A new OTP has been sent to your email."));
    }

    @Operation(summary = "Request Password Reset", description = "Sends a password reset OTP to the email.")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("If your email exists and is active, a password reset OTP has been sent."));
    }

    @Operation(summary = "Verify Password Reset OTP", description = "Verifies OTP and returns a short-lived resetToken to be used for setting new password.")
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<ApiResponse<String>> verifyResetOtp(@Valid @RequestBody VerifyResetOtpRequest request) {
        String resetToken = authService.verifyResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully. Use the reset token to change your password.", resetToken));
    }

    @Operation(summary = "Reset Password", description = "Sets a new password using the reset token. Logs the user out of all sessions.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Your previous sessions have been revoked."));
    }

}
