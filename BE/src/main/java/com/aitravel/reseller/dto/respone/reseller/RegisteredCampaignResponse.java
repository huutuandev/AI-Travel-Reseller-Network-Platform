package com.aitravel.reseller.dto.respone.reseller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegisteredCampaignResponse(
        UUID applicationId,
        UUID campaignId,
        String title,
        String description,
        String campaignStatus,
        String applicationStatus,
        String commissionType,
        BigDecimal commissionRate,
        Instant appliedAt) {
}