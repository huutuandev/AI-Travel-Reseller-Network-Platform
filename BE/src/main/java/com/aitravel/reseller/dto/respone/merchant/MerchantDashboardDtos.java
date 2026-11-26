package com.aitravel.reseller.dto.respone.merchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class MerchantDashboardDtos {

    private MerchantDashboardDtos() {
    }

    public record Overview(
            Period period,
            Summary summary,
            boolean clickTimeSeriesAvailable,
            List<NetworkPoint> networkPerformance,
            List<TopCampaign> topCampaigns,
            List<Activity> recentActivity
    ) {
    }

    public record Period(
            Instant from,
            Instant to,
            String timeZone
    ) {
    }

    public record Summary(
            long totalCampaigns,
            long activeResellers,
            long totalClicks,
            long approvedConversions,
            List<MoneyTotal> commissionTotals
    ) {
    }

    public record MoneyTotal(
            String currency,
            BigDecimal amount
    ) {
    }

    public record NetworkPoint(
            LocalDate date,
            Long clicks,
            long bookings,
            long conversions
    ) {
    }

    public record TopCampaign(
            String campaignId,
            String title,
            String productTitle,
            long conversions,
            long totalClicks,
            List<MoneyTotal> commissionTotals
    ) {
    }

    public record Activity(
            String type,
            String partnerName,
            String campaignTitle,
            String status,
            Instant occurredAt
    ) {
    }
}