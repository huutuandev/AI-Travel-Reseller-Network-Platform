package com.aitravel.reseller.dto.request.merchant;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMerchantBrandingRequest {

    @Size(max = 2000, message = "Logo URL must not exceed 2000 characters")
    private String logoUrl;

    @Size(max = 2000, message = "Favicon URL must not exceed 2000 characters")
    private String faviconUrl;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Primary color must be a valid HEX color code (e.g., #0B7F7B)")
    private String primaryColor;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Secondary color must be a valid HEX color code (e.g., #F4B63F)")
    private String secondaryColor;

    @Size(max = 10000, message = "Custom CSS must not exceed 10000 characters")
    private String customCss;

    @Size(max = 1000, message = "Footer text must not exceed 1000 characters")
    private String footerText;
}
