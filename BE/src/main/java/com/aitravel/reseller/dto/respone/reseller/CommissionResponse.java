package com.aitravel.reseller.dto.respone.reseller;

import java.math.BigDecimal;
import java.time.Instant;

public record CommissionResponse(
        String campaign,
        BigDecimal orderAmount,
        BigDecimal commissionAmount,
        String currency,
        String status,
        Instant createdAt) {
}