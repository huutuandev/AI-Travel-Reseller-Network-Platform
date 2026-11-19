package com.aitravel.reseller.dto.respone.merchant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class MerchantReportDtos {
    private MerchantReportDtos() {}

    public record CampaignPageResponse(
            List<CampaignReport> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record ResellerPageResponse(
            List<ResellerReport> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record CampaignReport(
            UUID campaignId,
            String campaignTitle,
            String campaignStatus,
            UUID productId,
            String productTitle,
            long totalClicks,
            BookingMetrics bookings,
            ConversionMetrics conversions,
            CommissionMetrics commissions
    ) {}

    public record ResellerReport(
            UUID resellerId,
            String resellerName,
            String email,
            long campaignCount,
            long applicationCount,
            long totalClicks,
            BookingMetrics bookings,
            ConversionMetrics conversions,
            CommissionMetrics commissions
    ) {}

    public record BookingMetrics(
            long total,
            long pending,
            long confirmed,
            long cancelled,
            long completed,
            long refunded,
            BigDecimal confirmedGmv,
            BigDecimal refundedAmount
    ) {}

    public record ConversionMetrics(
            long pending,
            long approved,
            long rejected,
            long reversed
    ) {}

    public record CommissionMetrics(
            BigDecimal pendingAmount,
            BigDecimal approvedAmount,
            BigDecimal payableAmount,
            BigDecimal partiallyPaidAmount,
            BigDecimal paidAmount,
            BigDecimal cancelledAmount,
            BigDecimal reversedAmount
    ) {}
}