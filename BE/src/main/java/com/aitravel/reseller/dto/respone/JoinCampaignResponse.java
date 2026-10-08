package com.aitravel.reseller.dto.respone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinCampaignResponse {
    private String message;
    private UUID campaignId;
    private UUID productId;
    private String trackingCode;
    private String landingSlug;
    private BigDecimal commissionSnapshot;
    private BigDecimal resellerSplitSnapshot;
}
