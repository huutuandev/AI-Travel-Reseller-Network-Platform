package com.aitravel.reseller.dto.respone.reseller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record ResellerDashboardResponse(
        Overview overview,
        List<CommissionSummary> commissionSummary,
        List<DailyClicks> clickTrend,
        List<CampaignSummary> campaigns,
        List<TrackingLinkSummary> trackingLinks,
        List<RecentCommission> recentCommissions) {

    public record Overview(
            long totalClicks,
            long totalConversions,
            BigDecimal conversionRate,
            List<MoneyAmount> pendingCommission,
            List<MoneyAmount> totalEarnings) {
    }

    public record MoneyAmount(String currency, BigDecimal amount) {
    }

    public record CommissionSummary(String status, String currency, BigDecimal amount) {
    }

    /** clicks is null because only an all-time click_count is persisted, not daily click events. */
    public record DailyClicks(LocalDate date, Long clicks, long conversions) {
    }

    public record CampaignSummary(String campaign, String status) {
    }

    public record TrackingLinkSummary(
            String campaign,
            String trackingUrl,
            long clicks,
            long conversions,
            Instant createdAt) {
    }

    public record RecentCommission(
            String campaign,
            BigDecimal orderAmount,
            BigDecimal commissionAmount,
            String currency,
            String status,
            Instant createdAt) {
    }
}
