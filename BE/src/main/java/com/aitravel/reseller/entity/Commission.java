package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "commissions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Commission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reseller_id", nullable = false)
    private Reseller reseller;

    @Column(name = "gross_commission", precision = 12, scale = 2)
    private BigDecimal grossCommission;

    @Column(name = "reseller_amount", precision = 12, scale = 2)
    private BigDecimal resellerAmount;

    @Column(name = "platform_amount", precision = 12, scale = 2)
    private BigDecimal platformAmount;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "eligible_at")
    private Instant eligibleAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
