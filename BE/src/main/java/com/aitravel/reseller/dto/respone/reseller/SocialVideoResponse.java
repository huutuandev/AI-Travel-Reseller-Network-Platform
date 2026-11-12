package com.aitravel.reseller.dto.respone.reseller;

import java.time.Instant;
import java.util.UUID;

public record SocialVideoResponse(
        UUID id,
        String platform,
        String externalVideoId,
        String postUrl,
        long views,
        long likes,
        String metricsStatus,
        Instant metricsFetchedAt,
        Instant createdAt) {
}
