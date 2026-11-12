package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_video_metric_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialVideoMetricSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private ResellerCampaignPost post;

    @Column(name = "views_count")
    private Long viewsCount;

    @Column(name = "likes_count")
    private Long likesCount;

    @Column(name = "fetched_at", nullable = false, updatable = false)
    private Instant fetchedAt;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "fetch_status", nullable = false, length = 20)
    private String fetchStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
