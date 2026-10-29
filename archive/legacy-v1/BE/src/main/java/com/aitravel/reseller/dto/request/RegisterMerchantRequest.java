package com.aitravel.reseller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class RegisterMerchantRequest {

    @Schema(description = "Merchant business or brand name", example = "Sun World Travel")
    @NotBlank(message = "Merchant name is required")
    @Size(max = 255, message = "Merchant name cannot exceed 255 characters")
    private String name;

    @Schema(description = "Contact phone number", example = "0901234567")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Schema(description = "Business address", example = "Da Nang, Vietnam")
    private String address;

    @Schema(description = "Business official website", example = "https://example.com")
    @Size(max = 255, message = "Website cannot exceed 255 characters")
    private String website;

    @Schema(description = "URL pointing to the merchant brand logo", example = "https://example.com/logo.png")
    private String logoUrl;
}
