package com.aitravel.reseller.dto.respone.reseller;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TikTokVideoItemResponse {
    private String id;
    private String title;
    private String description;
    private Instant createTime;
    private String coverImageUrl;
    private String shareUrl;
    private String embedLink;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
}
