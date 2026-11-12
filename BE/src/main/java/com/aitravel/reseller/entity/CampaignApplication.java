package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reseller_id", nullable = false)
    private ResellerProfile reseller;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "commission_type_snapshot", nullable = false, length = 20)
    private String commissionTypeSnapshot;

    @Column(name = "commission_rate_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionRateSnapshot;

    @CreationTimestamp
    @Column(name = "applied_at", updatable = false)
    private Instant appliedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;
}
