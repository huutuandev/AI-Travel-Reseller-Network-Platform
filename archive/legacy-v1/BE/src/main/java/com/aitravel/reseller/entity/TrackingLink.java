package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tracking_links")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TrackingLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reseller_id", nullable = false)
    private Reseller reseller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "tracking_code", length = 100, nullable = false, unique = true)
    private String trackingCode;

    @Column(name = "landing_slug", length = 150, nullable = false, unique = true)
    private String landingSlug;

    @Column(name = "commission_snapshot", precision = 5, scale = 2, nullable = false)
    private BigDecimal commissionSnapshot;

    @Column(name = "reseller_split_snapshot", precision = 5, scale = 2, nullable = false)
    private BigDecimal resellerSplitSnapshot;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
