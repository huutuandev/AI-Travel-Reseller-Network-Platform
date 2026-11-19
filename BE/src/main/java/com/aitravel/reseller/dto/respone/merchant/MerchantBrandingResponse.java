package com.aitravel.reseller.dto.respone.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantBrandingResponse {
    private UUID id;
    private UUID merchantId;
    private String logoUrl;
    private String faviconUrl;
    private String primaryColor;
    private String secondaryColor;
    private String customCss;
    private String footerText;
    private Instant updatedAt;
}
