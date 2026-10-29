package com.aitravel.reseller.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contents")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(length = 50)
    private String persona;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String hook;

    @Column(columnDefinition = "TEXT")
    private String script;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String cta;

    private String hashtags;

    @Column(name = "visual_prompt", columnDefinition = "TEXT")
    private String visualPrompt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
