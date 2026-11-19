package com.aitravel.reseller.dto.respone.reseller;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TikTokVideoListResponse {
    private UUID socialAccountId;
    private List<TikTokVideoItemResponse> videos;
    private Long cursor;
    private boolean hasMore;
}
