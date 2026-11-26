package com.aitravel.reseller.repository.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.Activity;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.MoneyTotal;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.NetworkPoint;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos.TopCampaign;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class MerchantDashboardQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MerchantDashboardQueryRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countCampaigns(UUID merchantId) {
        String sql = """
                SELECT COUNT(*)
                FROM travel_v2.campaigns c
                JOIN travel_v2.products p ON p.id = c.product_id
                WHERE p.merchant_id = :merchantId
                """;

        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("merchantId", merchantId),
                Long.class
        );
    }

    public long countActiveResellers(UUID merchantId) {
        String sql = """
                SELECT COUNT(DISTINCT ca.reseller_id)
                FROM travel_v2.campaign_applications ca
                JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                JOIN travel_v2.reseller_profiles rp ON rp.id = ca.reseller_id
                WHERE p.merchant_id = :merchantId
                  AND ca.status = 'APPROVED'
                  AND rp.status = 'ACTIVE'
                """;

        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("merchantId", merchantId),
                Long.class
        );
    }

    public long sumLifetimeClicks(UUID merchantId) {
        String sql = """
                SELECT COALESCE(SUM(tl.click_count), 0)
                FROM travel_v2.tracking_links tl
                JOIN travel_v2.campaign_applications ca
                  ON ca.id = tl.campaign_application_id
                JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                WHERE p.merchant_id = :merchantId
                """;

        Long result = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("merchantId", merchantId),
                Long.class
        );

        return result == null ? 0 : result;
    }

    public long countApprovedConversions(
            UUID merchantId,
            Instant from,
            Instant to
    ) {
        String sql = """
                SELECT COUNT(*)
                FROM bookings b
                WHERE b.merchant_id = :merchantId
                  AND b.tracking_link_id IS NOT NULL
                  AND b.created_at >= :from
                  AND b.created_at < :to
                  AND b.booking_status IN ('CONFIRMED', 'COMPLETED')
                """;

        Long result = jdbcTemplate.queryForObject(
                sql,
                parameters(merchantId, from, to),
                Long.class
        );

        return result == null ? 0 : result;
    }

    public List<MoneyTotal> findCommissionTotals(
            UUID merchantId,
            Instant from,
            Instant to
    ) {
        String sql = """
                SELECT TRIM(cm.currency) AS currency,
                       COALESCE(SUM(cm.amount), 0) AS amount
                FROM commissions cm
                JOIN bookings b ON b.id = cm.booking_id
                WHERE b.merchant_id = :merchantId
                  AND cm.created_at >= :from
                  AND cm.created_at < :to
                  AND cm.status NOT IN ('CANCELLED', 'REVERSED')
                GROUP BY TRIM(cm.currency)
                ORDER BY TRIM(cm.currency)
                """;

        return jdbcTemplate.query(
                sql,
                parameters(merchantId, from, to),
                (rs, rowNum) -> new MoneyTotal(
                        rs.getString("currency"),
                        rs.getBigDecimal("amount")
                )
        );
    }

    public List<NetworkPoint> findDailyBookingMetrics(
            UUID merchantId,
            Instant from,
            Instant to
    ) {
        String sql = """
                SELECT (b.created_at AT TIME ZONE 'UTC')::date AS activity_date,
                       COUNT(*) AS bookings,
                       COUNT(*) FILTER (
                           WHERE b.booking_status IN ('CONFIRMED', 'COMPLETED')
                       ) AS conversions
                FROM bookings b
                WHERE b.merchant_id = :merchantId
                  AND b.tracking_link_id IS NOT NULL
                  AND b.created_at >= :from
                  AND b.created_at < :to
                GROUP BY (b.created_at AT TIME ZONE 'UTC')::date
                ORDER BY activity_date
                """;

        return jdbcTemplate.query(
                sql,
                parameters(merchantId, from, to),
                (rs, rowNum) -> new NetworkPoint(
                        rs.getObject("activity_date", LocalDate.class),
                        null,
                        rs.getLong("bookings"),
                        rs.getLong("conversions")
                )
        );
    }

    public List<TopCampaign> findTopCampaigns(
            UUID merchantId,
            Instant from,
            Instant to
    ) {
        String sql = """
                SELECT c.id AS campaign_id,
                       c.title AS campaign_title,
                       p.title AS product_title,
                       COALESCE((
                           SELECT COUNT(*)
                           FROM bookings b
                           JOIN tracking_links tl
                             ON tl.id = b.tracking_link_id
                           JOIN campaign_applications ca
                             ON ca.id = tl.campaign_application_id
                           WHERE ca.campaign_id = c.id
                             AND b.merchant_id = :merchantId
                             AND b.created_at >= :from
                             AND b.created_at < :to
                             AND b.booking_status IN ('CONFIRMED', 'COMPLETED')
                       ), 0) AS conversions,
                       COALESCE((
                           SELECT SUM(tl.click_count)
                           FROM tracking_links tl
                           JOIN campaign_applications ca
                             ON ca.id = tl.campaign_application_id
                           WHERE ca.campaign_id = c.id
                       ), 0) AS total_clicks
                FROM campaigns c
                JOIN products p ON p.id = c.product_id
                WHERE p.merchant_id = :merchantId
                ORDER BY conversions DESC, c.created_at DESC
                LIMIT 5
                """;

        return jdbcTemplate.query(
                sql,
                parameters(merchantId, from, to),
                (rs, rowNum) -> new TopCampaign(
                        rs.getObject("campaign_id").toString(),
                        rs.getString("campaign_title"),
                        rs.getString("product_title"),
                        rs.getLong("conversions"),
                        rs.getLong("total_clicks"),
                        List.of()
                )
        );
    }

    public List<MoneyTotal> findCampaignCommissionTotals(
            UUID merchantId,
            UUID campaignId,
            Instant from,
            Instant to
    ) {
        String sql = """
                SELECT TRIM(cm.currency) AS currency,
                       COALESCE(SUM(cm.amount), 0) AS amount
                FROM commissions cm
                JOIN bookings b ON b.id = cm.booking_id
                JOIN tracking_links tl ON tl.id = b.tracking_link_id
                JOIN campaign_applications ca
                  ON ca.id = tl.campaign_application_id
                WHERE b.merchant_id = :merchantId
                  AND ca.campaign_id = :campaignId
                  AND cm.created_at >= :from
                  AND cm.created_at < :to
                  AND cm.status NOT IN ('CANCELLED', 'REVERSED')
                GROUP BY TRIM(cm.currency)
                ORDER BY TRIM(cm.currency)
                """;

        MapSqlParameterSource parameters = parameters(merchantId, from, to)
                .addValue("campaignId", campaignId);

        return jdbcTemplate.query(
                sql,
                parameters,
                (rs, rowNum) -> new MoneyTotal(
                        rs.getString("currency"),
                        rs.getBigDecimal("amount")
                )
        );
    }

    public List<Activity> findRecentActivity(UUID merchantId) {
        String sql = """
                SELECT activity_type,
                    partner_name,
                    campaign_title,
                    status,
                    occurred_at
                FROM (
                    SELECT 'Booking recorded' AS activity_type,
                        u.full_name AS partner_name,
                        c.title AS campaign_title,
                        b.booking_status AS status,
                        b.created_at AS occurred_at
                    FROM travel_v2.bookings b
                    JOIN travel_v2.tracking_links tl
                    ON tl.id = b.tracking_link_id
                    JOIN travel_v2.campaign_applications ca
                    ON ca.id = tl.campaign_application_id
                    JOIN travel_v2.campaigns c
                    ON c.id = ca.campaign_id
                    JOIN travel_v2.products p
                    ON p.id = c.product_id
                    JOIN travel_v2.reseller_profiles rp
                    ON rp.id = ca.reseller_id
                    JOIN travel_v2.users u
                    ON u.id = rp.user_id
                    WHERE b.merchant_id = :merchantId
                    AND p.merchant_id = :merchantId

                    UNION ALL

                    SELECT 'Reseller applied' AS activity_type,
                        u.full_name AS partner_name,
                        c.title AS campaign_title,
                        ca.status AS status,
                        ca.applied_at AS occurred_at
                    FROM travel_v2.campaign_applications ca
                    JOIN travel_v2.campaigns c
                    ON c.id = ca.campaign_id
                    JOIN travel_v2.products p
                    ON p.id = c.product_id
                    JOIN travel_v2.reseller_profiles rp
                    ON rp.id = ca.reseller_id
                    JOIN travel_v2.users u
                    ON u.id = rp.user_id
                    WHERE p.merchant_id = :merchantId

                    UNION ALL

                    SELECT 'Campaign created' AS activity_type,
                        NULL::text AS partner_name,
                        c.title AS campaign_title,
                        c.status AS status,
                        c.created_at AS occurred_at
                    FROM travel_v2.campaigns c
                    JOIN travel_v2.products p
                    ON p.id = c.product_id
                    WHERE p.merchant_id = :merchantId
                ) activity
                ORDER BY occurred_at DESC
                LIMIT 10
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("merchantId", merchantId),
                (rs, rowNum) -> new Activity(
                        rs.getString("activity_type"),
                        rs.getString("partner_name"),
                        rs.getString("campaign_title"),
                        rs.getString("status"),
                        toInstant(rs.getTimestamp("occurred_at"))
                )
        );
    }

    private MapSqlParameterSource parameters(
            UUID merchantId,
            Instant from,
            Instant to
    ) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}