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
public class UpdateMerchantProfileRequest {

    @NotBlank(message = "Business name is required")
    @Size(max = 255, message = "Business name must not exceed 255 characters")
    private String businessName;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Invalid contact email format")
    @Size(max = 255, message = "Contact email must not exceed 255 characters")
    private String contactEmail;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @Size(max = 255, message = "Official website domain must not exceed 255 characters")
    private String customDomain;

    @Size(max = 255, message = "Owner full name must not exceed 255 characters")
    private String ownerFullName;

    @Size(max = 50, message = "Owner phone must not exceed 50 characters")
    private String ownerPhone;
}
