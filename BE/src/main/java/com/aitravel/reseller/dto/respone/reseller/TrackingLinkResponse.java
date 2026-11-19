package com.aitravel.reseller.dto.respone.reseller;

import java.time.Instant;

public record TrackingLinkResponse(
        String campaign,
        String trackingUrl,
        long clicks,
        long conversions,
        Instant created) {
}