package com.aitravel.reseller.dto.respone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private UUID id;
    private UUID merchantId;
    private String merchantName;
    private UUID productId;
    private String productName;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal commissionRate;
    private BigDecimal resellerSplitRate;
    private String guidelines;
    private String status;
    private Instant createdAt;
}
