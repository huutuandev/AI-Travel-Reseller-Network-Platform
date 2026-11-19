package com.aitravel.reseller.repository.reseller;

import com.aitravel.reseller.dto.respone.reseller.ResellerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ResellerDashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${app.tracking.base-url}")
    private String trackingBaseUrl;

    public long totalClicks(UUID resellerId) {
        Long result = jdbc.queryForObject("""
                SELECT COALESCE(SUM(link.click_count), 0)
                FROM travel_v2.tracking_links link
                JOIN travel_v2.campaign_applications application
                  ON application.id = link.campaign_application_id
                WHERE application.reseller_id = :resellerId
                """, Map.of("resellerId", resellerId), Long.class);
        return result == null ? 0 : result;
    }

    public long totalConversions(UUID resellerId) {
        Long result = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM travel_v2.bookings booking
                JOIN travel_v2.tracking_links link ON link.id = booking.tracking_link_id
                JOIN travel_v2.campaign_applications application
                  ON application.id = link.campaign_application_id
                WHERE application.reseller_id = :resellerId
                  AND booking.booking_status NOT IN ('CANCELLED', 'REFUNDED')
                """, Map.of("resellerId", resellerId), Long.class);
        return result == null ? 0 : result;
    }

    public List<ResellerDashboardResponse.MoneyAmount> commissionAmounts(
            UUID resellerId, List<String> statuses) {
        return jdbc.query("""
                SELECT currency, SUM(amount) AS amount
                FROM travel_v2.commissions
                WHERE reseller_id = :resellerId
                  AND status IN (:statuses)
                GROUP BY currency
                ORDER BY currency
                """, Map.of("resellerId", resellerId, "statuses", statuses),
                (rs, rowNum) -> new ResellerDashboardResponse.MoneyAmount(
                        rs.getString("currency").trim(), rs.getBigDecimal("amount")));
    }

    public List<ResellerDashboardResponse.DailyClicks> clickTrend(UUID resellerId) {
        return jdbc.query("""
                WITH days AS (
                    SELECT generate_series(
                        CURRENT_DATE - INTERVAL '29 days', CURRENT_DATE, INTERVAL '1 day'
                    )::date AS day
                ), daily_conversions AS (
                    SELECT booking.created_at::date AS day, COUNT(*) AS conversions
                    FROM travel_v2.bookings booking
                    JOIN travel_v2.tracking_links link ON link.id = booking.tracking_link_id
                    JOIN travel_v2.campaign_applications application
                      ON application.id = link.campaign_application_id
                    WHERE application.reseller_id = :resellerId
                      AND booking.booking_status NOT IN ('CANCELLED', 'REFUNDED')
                      AND booking.created_at >= CURRENT_DATE - INTERVAL '29 days'
                      AND booking.created_at < CURRENT_DATE + INTERVAL '1 day'
                    GROUP BY booking.created_at::date
                )
                SELECT days.day,
                       COALESCE(daily_conversions.conversions, 0) AS conversions
                FROM days
                LEFT JOIN daily_conversions ON daily_conversions.day = days.day
                ORDER BY days.day
                """, Map.of("resellerId", resellerId),
                (rs, rowNum) -> new ResellerDashboardResponse.DailyClicks(
                        rs.getDate("day").toLocalDate(), null, rs.getLong("conversions")));
    }

    public List<ResellerDashboardResponse.CampaignSummary> campaigns(UUID resellerId) {
        return jdbc.query("""
                SELECT DISTINCT campaign.title, campaign.status
                FROM travel_v2.campaign_applications application
                JOIN travel_v2.campaigns campaign ON campaign.id = application.campaign_id
                WHERE application.reseller_id = :resellerId
                  AND application.status = 'APPROVED'
                  AND campaign.status = 'ACTIVE'
                ORDER BY campaign.title
                LIMIT 5
                """, Map.of("resellerId", resellerId),
                (rs, rowNum) -> new ResellerDashboardResponse.CampaignSummary(
                        rs.getString("title"), rs.getString("status")));
    }

    public List<ResellerDashboardResponse.TrackingLinkSummary> trackingLinks(UUID resellerId) {
        String baseUrl = trackingBaseUrl.replaceAll("/+$", "");
        return jdbc.query("""
                SELECT campaign.title AS campaign,
                       link.tracking_code,
                       link.created_at,
                       link.click_count AS clicks,
                       COALESCE(bookings.conversion_count, 0) AS conversions
                FROM travel_v2.tracking_links link
                JOIN travel_v2.campaign_applications application
                  ON application.id = link.campaign_application_id
                JOIN travel_v2.campaigns campaign ON campaign.id = application.campaign_id
                LEFT JOIN (
                    SELECT tracking_link_id, COUNT(*) AS conversion_count
                    FROM travel_v2.bookings
                    WHERE booking_status NOT IN ('CANCELLED', 'REFUNDED')
                    GROUP BY tracking_link_id
                ) bookings ON bookings.tracking_link_id = link.id
                WHERE application.reseller_id = :resellerId
                ORDER BY link.created_at DESC
                LIMIT 5
                """, Map.of("resellerId", resellerId),
                (rs, rowNum) -> new ResellerDashboardResponse.TrackingLinkSummary(
                        rs.getString("campaign"),
                        baseUrl + "/r/" + rs.getString("tracking_code"),
                        rs.getLong("clicks"),
                        rs.getLong("conversions"),
                        toInstant(rs.getTimestamp("created_at"))));
    }

    public List<ResellerDashboardResponse.RecentCommission> recentCommissions(UUID resellerId) {
        return jdbc.query("""
                SELECT campaign.title AS campaign,
                       booking.total_amount AS order_amount,
                       commission.amount AS commission_amount,
                       commission.currency,
                       commission.status,
                       commission.created_at
                FROM travel_v2.commissions commission
                JOIN travel_v2.bookings booking ON booking.id = commission.booking_id
                LEFT JOIN travel_v2.tracking_links link ON link.id = booking.tracking_link_id
                LEFT JOIN travel_v2.campaign_applications application
                  ON application.id = link.campaign_application_id
                LEFT JOIN travel_v2.campaigns campaign ON campaign.id = application.campaign_id
                WHERE commission.reseller_id = :resellerId
                ORDER BY commission.created_at DESC
                LIMIT 5
                """, Map.of("resellerId", resellerId),
                (rs, rowNum) -> new ResellerDashboardResponse.RecentCommission(
                        rs.getString("campaign"),
                        rs.getBigDecimal("order_amount"),
                        rs.getBigDecimal("commission_amount"),
                        rs.getString("currency").trim(),
                        rs.getString("status"),
                        toInstant(rs.getTimestamp("created_at"))));
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
