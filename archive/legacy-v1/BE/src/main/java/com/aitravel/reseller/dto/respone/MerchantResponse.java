package com.aitravel.reseller.dto.respone;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class MerchantResponse {

    @Schema(description = "Merchant ID")
    private UUID id;

    @Schema(description = "User ID associated with merchant")
    private UUID userId;

    @Schema(description = "Merchant business name")
    private String name;

    @Schema(description = "Merchant logo URL")
    private String logoUrl;

    @Schema(description = "Merchant contact phone")
    private String phone;

    @Schema(description = "Merchant address")
    private String address;

    @Schema(description = "Merchant official website")
    private String website;

    @Schema(description = "Verification status", example = "PENDING")
    private String verificationStatus;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
