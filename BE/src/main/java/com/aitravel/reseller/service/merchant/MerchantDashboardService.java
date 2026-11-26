package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.Activity;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.MoneyTotal;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.NetworkPoint;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.Overview;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.Period;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.Summary;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.TopCampaign;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.MerchantDashboardQueryRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantDashboardService {

    private static final Duration DEFAULT_PERIOD = Duration.ofDays(30);
    private static final Duration MAX_PERIOD = Duration.ofDays(90);

    private final MerchantRepository merchantRepository;
    private final MerchantDashboardQueryRepository dashboardRepository;

    @Transactional(readOnly = true)
    public Overview getOverview(
            CustomUserDetails user,
            Instant requestedFrom,
            Instant requestedTo
    ) {
        Merchant merchant = merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Current user is not a merchant"
                ));

        Instant to = requestedTo == null ? Instant.now() : requestedTo;
        Instant from = requestedFrom == null
                ? to.minus(DEFAULT_PERIOD)
                : requestedFrom;

        validatePeriod(from, to);

        UUID merchantId = merchant.getId();

        long campaignCount = dashboardRepository.countCampaigns(merchantId);
        long activeResellerCount =
                dashboardRepository.countActiveResellers(merchantId);
        long totalClicks = dashboardRepository.sumLifetimeClicks(merchantId);
        long conversions =
                dashboardRepository.countApprovedConversions(merchantId, from, to);

        List<MoneyTotal> commissionTotals =
                dashboardRepository.findCommissionTotals(merchantId, from, to);

        List<NetworkPoint> networkPerformance =
                fillMissingDates(dashboardRepository.findDailyBookingMetrics(
                        merchantId,
                        from,
                        to
                ), from, to);

        List<TopCampaign> topCampaigns =
                dashboardRepository.findTopCampaigns(merchantId, from, to)
                        .stream()
                        .map(campaign -> {
                            UUID campaignId = UUID.fromString(campaign.campaignId());
                            List<MoneyTotal> campaignCommissions =
                                    dashboardRepository.findCampaignCommissionTotals(
                                            merchantId,
                                            campaignId,
                                            from,
                                            to
                                    );

                            return new TopCampaign(
                                    campaign.campaignId(),
                                    campaign.title(),
                                    campaign.productTitle(),
                                    campaign.conversions(),
                                    campaign.totalClicks(),
                                    campaignCommissions
                            );
                        })
                        .toList();

        List<Activity> recentActivity =
                dashboardRepository.findRecentActivity(merchantId);

        return new Overview(
                new Period(from, to, "UTC"),
                new Summary(
                        campaignCount,
                        activeResellerCount,
                        totalClicks,
                        conversions,
                        commissionTotals
                ),
                false,
                networkPerformance,
                topCampaigns,
                recentActivity
        );
    }

    private void validatePeriod(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "'from' must be earlier than 'to'"
            );
        }

        if (Duration.between(from, to).compareTo(MAX_PERIOD) > 0) {
            throw new IllegalArgumentException(
                    "Dashboard period cannot exceed 90 days"
            );
        }
    }

    private List<NetworkPoint> fillMissingDates(
            List<NetworkPoint> databasePoints,
            Instant from,
            Instant to
    ) {
        Map<LocalDate, NetworkPoint> pointsByDate = new HashMap<>();
        for (NetworkPoint point : databasePoints) {
            pointsByDate.put(point.date(), point);
        }

        LocalDate firstDate = from.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate lastDate = to.minusNanos(1)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        List<NetworkPoint> result = new ArrayList<>();
        for (LocalDate date = firstDate;
             !date.isAfter(lastDate);
             date = date.plusDays(1)) {

            NetworkPoint point = pointsByDate.get(date);
            if (point == null) {
                result.add(new NetworkPoint(date, null, 0, 0));
            } else {
                result.add(point);
            }
        }

        return result;
    }
}