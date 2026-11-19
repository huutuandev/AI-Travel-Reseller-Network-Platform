package com.aitravel.reseller.dto.request.merchant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOnboardingCompleteRequest {

    @NotBlank(message = "Onboarding token is required")
    private String onboardingToken;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;

    @Size(max = 100, message = "Business code must not exceed 100 characters")
    private String code;

    @NotBlank(message = "Subdomain is required")
    @Size(min = 3, max = 100, message = "Subdomain must be between 3 and 100 characters")
    private String subdomain;

    @Size(max = 255, message = "Official website domain must not exceed 255 characters")
    private String customDomain;

    @Email(message = "Invalid contact email format")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
}
