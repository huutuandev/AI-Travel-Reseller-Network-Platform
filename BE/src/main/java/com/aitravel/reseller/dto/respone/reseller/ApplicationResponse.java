package com.aitravel.reseller.dto.respone.reseller;

import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID campaignId,
        String status,
        Instant appliedAt) {
}