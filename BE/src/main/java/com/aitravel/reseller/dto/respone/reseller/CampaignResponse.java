package com.aitravel.reseller.dto.respone.reseller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String title,
        String description,
        String status,
        String commissionType,
        BigDecimal commissionRate,
        String commissionCurrency,
        Instant startDate,
        Instant endDate) {
}