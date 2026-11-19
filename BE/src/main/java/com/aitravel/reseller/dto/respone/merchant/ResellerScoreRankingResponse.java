package com.aitravel.reseller.dto.respone.merchant;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResellerScoreRankingResponse(Instant fetchedAt, List<Entry> rankings) {
    public record Entry(int rank, UUID resellerId, String resellerName, long videoCount,
                        long likes, long shares, long comments, long points) {}
}
