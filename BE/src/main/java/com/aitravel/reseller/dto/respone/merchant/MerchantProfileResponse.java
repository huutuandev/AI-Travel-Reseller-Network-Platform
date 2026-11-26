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
public class MerchantProfileResponse {
    private UUID id;
    private String code;
    private String businessName;
    private String description;
    private String contactEmail;
    private String phone;
    private String address;
    private String subdomain;
    private String customDomain;
    private String status;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    private UUID ownerId;
    private String ownerEmail;
    private String ownerFullName;
    private String ownerPhone;

    private MerchantBrandingResponse branding;
}
