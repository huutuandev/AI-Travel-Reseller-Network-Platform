package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reseller_id", nullable = false)
    private ResellerProfile reseller;

    @Column(nullable = false, length = 30)
    private String platform;

    @Column(nullable = false, length = 150)
    private String username;

    @Column(name = "profile_url", columnDefinition = "TEXT")
    private String profileUrl;

    @Column(name = "followers_count", nullable = false)
    @Builder.Default
    private Long followersCount = 0L;

    @Column(name = "average_views", nullable = false)
    @Builder.Default
    private Long averageViews = 0L;

    @Column(name = "metrics_source", nullable = false, length = 20)
    @Builder.Default
    private String metricsSource = "MANUAL";

    @Column(name = "metrics_updated_at")
    private Instant metricsUpdatedAt;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
