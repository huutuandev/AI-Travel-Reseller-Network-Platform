package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_branding")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantBranding {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false, unique = true)
    private Merchant merchant;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "favicon_url", columnDefinition = "TEXT")
    private String faviconUrl;

    @Column(name = "primary_color", length = 20)
    @Builder.Default
    private String primaryColor = "#0B7F7B";

    @Column(name = "secondary_color", length = 20)
    @Builder.Default
    private String secondaryColor = "#F4B63F";

    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
