package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reseller_campaign_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResellerCampaignPost {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_application_id", nullable = false)
    private CampaignApplication campaignApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "social_account_id", nullable = false)
    private SocialAccount socialAccount;

    @Column(name = "post_url", nullable = false, columnDefinition = "TEXT")
    private String postUrl;

    @Column(nullable = false, length = 30)
    private String platform;

    @Column(name = "external_video_id", length = 255)
    private String externalVideoId;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private Long viewsCount = 0L;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Long likesCount = 0L;

    @Column(name = "metrics_source", length = 30)
    private String metricsSource;

    @Column(name = "metrics_status", nullable = false, length = 20)
    @Builder.Default
    private String metricsStatus = "PENDING";

    @Column(name = "metrics_fetched_at")
    private Instant metricsFetchedAt;

    @Column(name = "metrics_error", columnDefinition = "TEXT")
    private String metricsError;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private Long commentsCount = 0L;

    @Column(name = "shares_count", nullable = false)
    @Builder.Default
    private Long sharesCount = 0L;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PUBLISHED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
