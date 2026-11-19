package com.aitravel.reseller.repository.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantReportDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MerchantReportQueryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public long countCampaignReports(
            UUID merchantId,
            String search,
            String status,
            UUID productId,
            UUID campaignId
    ) {
        MapSqlParameterSource params = campaignParameters(
                merchantId, search, status, productId, campaignId, null, null
        );

        String sql = """
                SELECT COUNT(*)
                FROM travel_v2.campaigns c
                JOIN travel_v2.products p ON p.id = c.product_id
                """ + campaignWhere(search, status, productId, campaignId);

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<MerchantReportDtos.CampaignReport> findCampaignReportPage(
            UUID merchantId,
            String search,
            String status,
            UUID productId,
            UUID campaignId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        MapSqlParameterSource params = campaignParameters(
                merchantId, search, status, productId, campaignId, from, to
        );
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);

        String sql = """
                WITH campaign_base AS (
                    SELECT
                        c.id AS campaign_id,
                        c.title AS campaign_title,
                        c.status AS campaign_status,
                        p.id AS product_id,
                        p.title AS product_title
                    FROM travel_v2.campaigns c
                    JOIN travel_v2.products p ON p.id = c.product_id
                    """ + campaignWhere(search, status, productId, campaignId) + """
                ),
                clicks AS (
                    SELECT
                        ca.campaign_id,
                        SUM(tl.click_count)::BIGINT AS total_clicks
                    FROM travel_v2.tracking_links tl
                    JOIN travel_v2.campaign_applications ca
                        ON ca.id = tl.campaign_application_id
                    JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                    JOIN travel_v2.products p ON p.id = c.product_id
                    WHERE p.merchant_id = :merchantId
                    GROUP BY ca.campaign_id
                ),
                booking_metrics AS (
                    SELECT
                        ca.campaign_id,
                        COUNT(*) AS booking_total,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'PENDING'
                        ) AS booking_pending,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'CONFIRMED'
                        ) AS booking_confirmed,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'CANCELLED'
                        ) AS booking_cancelled,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'COMPLETED'
                        ) AS booking_completed,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'REFUNDED'
                        ) AS booking_refunded,
                        COALESCE(
                            SUM(b.total_amount) FILTER (
                                WHERE b.booking_status IN ('CONFIRMED', 'COMPLETED')
                            ),
                            0
                        ) AS confirmed_gmv,
                        COALESCE(
                            SUM(b.total_amount) FILTER (
                                WHERE b.booking_status = 'REFUNDED'
                            ),
                            0
                        ) AS refunded_amount,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'PENDING'
                        ) AS conversion_pending,
                        COUNT(*) FILTER (
                            WHERE b.booking_status IN ('CONFIRMED', 'COMPLETED')
                        ) AS conversion_approved,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'CANCELLED'
                        ) AS conversion_rejected,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'REFUNDED'
                        ) AS conversion_reversed
                    FROM travel_v2.bookings b
                    JOIN travel_v2.tracking_links tl
                        ON tl.id = b.tracking_link_id
                    JOIN travel_v2.campaign_applications ca
                        ON ca.id = tl.campaign_application_id
                    JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                    JOIN travel_v2.products p ON p.id = c.product_id
                    WHERE b.merchant_id = :merchantId
                      AND p.merchant_id = :merchantId
                      AND b.created_at >= COALESCE(
                          CAST(:from AS TIMESTAMPTZ),
                          '-infinity'::TIMESTAMPTZ
                      )
                      AND b.created_at < COALESCE(
                          CAST(:to AS TIMESTAMPTZ),
                          'infinity'::TIMESTAMPTZ
                      )
                    GROUP BY ca.campaign_id
                ),
                commission_metrics AS (
                    SELECT
                        ca.campaign_id,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'PENDING'),
                            0
                        ) AS commission_pending,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'APPROVED'),
                            0
                        ) AS commission_approved,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'PAYABLE'),
                            0
                        ) AS commission_payable,
                        COALESCE(
                            SUM(cm.amount) FILTER (
                                WHERE cm.status = 'PARTIALLY_PAID'
                            ),
                            0
                        ) AS commission_partially_paid,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'PAID'),
                            0
                        ) AS commission_paid,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'CANCELLED'),
                            0
                        ) AS commission_cancelled,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'REVERSED'),
                            0
                        ) AS commission_reversed
                    FROM travel_v2.commissions cm
                    JOIN travel_v2.bookings b ON b.id = cm.booking_id
                    JOIN travel_v2.tracking_links tl
                        ON tl.id = b.tracking_link_id
                    JOIN travel_v2.campaign_applications ca
                        ON ca.id = tl.campaign_application_id
                    JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                    JOIN travel_v2.products p ON p.id = c.product_id
                    WHERE b.merchant_id = :merchantId
                      AND p.merchant_id = :merchantId
                      AND cm.created_at >= COALESCE(
                          CAST(:from AS TIMESTAMPTZ),
                          '-infinity'::TIMESTAMPTZ
                      )
                      AND cm.created_at < COALESCE(
                          CAST(:to AS TIMESTAMPTZ),
                          'infinity'::TIMESTAMPTZ
                      )
                    GROUP BY ca.campaign_id
                )
                SELECT
                    cb.campaign_id,
                    cb.campaign_title,
                    cb.campaign_status,
                    cb.product_id,
                    cb.product_title,
                    COALESCE(cl.total_clicks, 0) AS total_clicks,
                    COALESCE(bm.booking_total, 0) AS booking_total,
                    COALESCE(bm.booking_pending, 0) AS booking_pending,
                    COALESCE(bm.booking_confirmed, 0) AS booking_confirmed,
                    COALESCE(bm.booking_cancelled, 0) AS booking_cancelled,
                    COALESCE(bm.booking_completed, 0) AS booking_completed,
                    COALESCE(bm.booking_refunded, 0) AS booking_refunded,
                    COALESCE(bm.confirmed_gmv, 0) AS confirmed_gmv,
                    COALESCE(bm.refunded_amount, 0) AS refunded_amount,
                    COALESCE(bm.conversion_pending, 0) AS conversion_pending,
                    COALESCE(bm.conversion_approved, 0) AS conversion_approved,
                    COALESCE(bm.conversion_rejected, 0) AS conversion_rejected,
                    COALESCE(bm.conversion_reversed, 0) AS conversion_reversed,
                    COALESCE(cm.commission_pending, 0) AS commission_pending,
                    COALESCE(cm.commission_approved, 0) AS commission_approved,
                    COALESCE(cm.commission_payable, 0) AS commission_payable,
                    COALESCE(cm.commission_partially_paid, 0)
                        AS commission_partially_paid,
                    COALESCE(cm.commission_paid, 0) AS commission_paid,
                    COALESCE(cm.commission_cancelled, 0) AS commission_cancelled,
                    COALESCE(cm.commission_reversed, 0) AS commission_reversed
                FROM campaign_base cb
                LEFT JOIN clicks cl ON cl.campaign_id = cb.campaign_id
                LEFT JOIN booking_metrics bm ON bm.campaign_id = cb.campaign_id
                LEFT JOIN commission_metrics cm ON cm.campaign_id = cb.campaign_id
                ORDER BY cb.campaign_title, cb.campaign_id
                LIMIT :limit OFFSET :offset
                """;

        return jdbc.query(sql, params, this::mapCampaignReport);
    }

    public long countResellerReports(
            UUID merchantId,
            String search,
            UUID campaignId
    ) {
        MapSqlParameterSource params = resellerParameters(
                merchantId, search, campaignId, null, null
        );

        String sql = """
                SELECT COUNT(DISTINCT rp.id)
                FROM travel_v2.reseller_profiles rp
                JOIN travel_v2.users u ON u.id = rp.user_id
                JOIN travel_v2.campaign_applications ca
                    ON ca.reseller_id = rp.id
                JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                """ + resellerWhere(search, campaignId);

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<MerchantReportDtos.ResellerReport> findResellerReportPage(
            UUID merchantId,
            String search,
            UUID campaignId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        MapSqlParameterSource params = resellerParameters(
                merchantId, search, campaignId, from, to
        );
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);

        String sql = """
                WITH reseller_base AS (
                    SELECT
                        rp.id AS reseller_id,
                        u.full_name AS reseller_name,
                        u.email,
                        COUNT(DISTINCT ca.campaign_id) AS campaign_count,
                        COUNT(DISTINCT ca.id) AS application_count
                    FROM travel_v2.reseller_profiles rp
                    JOIN travel_v2.users u ON u.id = rp.user_id
                    JOIN travel_v2.campaign_applications ca
                        ON ca.reseller_id = rp.id
                    JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                    JOIN travel_v2.products p ON p.id = c.product_id
                    """ + resellerWhere(search, campaignId) + """
                    GROUP BY rp.id, u.full_name, u.email
                ),
                clicks AS (
                    SELECT
                        ca.reseller_id,
                        SUM(tl.click_count)::BIGINT AS total_clicks
                    FROM travel_v2.tracking_links tl
                    JOIN travel_v2.campaign_applications ca
                        ON ca.id = tl.campaign_application_id
                    JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                    JOIN travel_v2.products p ON p.id = c.product_id
                    WHERE p.merchant_id = :merchantId
                    """ + resellerCampaignFilter(campaignId) + """
                    GROUP BY ca.reseller_id
                ),
                booking_metrics AS (
                    SELECT
                        ca.reseller_id,
                        COUNT(*) AS booking_total,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'PENDING'
                        ) AS booking_pending,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'CONFIRMED'
                        ) AS booking_confirmed,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'CANCELLED'
                        ) AS booking_cancelled,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'COMPLETED'
                        ) AS booking_completed,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'REFUNDED'
                        ) AS booking_refunded,
                        COALESCE(
                            SUM(b.total_amount) FILTER (
                                WHERE b.booking_status IN ('CONFIRMED', 'COMPLETED')
                            ),
                            0
                        ) AS confirmed_gmv,
                        COALESCE(
                            SUM(b.total_amount) FILTER (
                                WHERE b.booking_status = 'REFUNDED'
                            ),
                            0
                        ) AS refunded_amount,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'PENDING'
                        ) AS conversion_pending,
                        COUNT(*) FILTER (
                            WHERE b.booking_status IN ('CONFIRMED', 'COMPLETED')
                        ) AS conversion_approved,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'CANCELLED'
                        ) AS conversion_rejected,
                        COUNT(*) FILTER (
                            WHERE b.booking_status = 'REFUNDED'
                        ) AS conversion_reversed
                    FROM travel_v2.bookings b
                    JOIN travel_v2.tracking_links tl
                        ON tl.id = b.tracking_link_id
                    JOIN travel_v2.campaign_applications ca
                        ON ca.id = tl.campaign_application_id
                    JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                    JOIN travel_v2.products p ON p.id = c.product_id
                    WHERE b.merchant_id = :merchantId
                      AND p.merchant_id = :merchantId
                      AND b.created_at >= COALESCE(
                          CAST(:from AS TIMESTAMPTZ),
                          '-infinity'::TIMESTAMPTZ
                      )
                      AND b.created_at < COALESCE(
                          CAST(:to AS TIMESTAMPTZ),
                          'infinity'::TIMESTAMPTZ
                      )
                    """ + resellerCampaignFilter(campaignId) + """
                    GROUP BY ca.reseller_id
                ),
                commission_metrics AS (
                    SELECT
                        ca.reseller_id,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'PENDING'),
                            0
                        ) AS commission_pending,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'APPROVED'),
                            0
                        ) AS commission_approved,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'PAYABLE'),
                            0
                        ) AS commission_payable,
                        COALESCE(
                            SUM(cm.amount) FILTER (
                                WHERE cm.status = 'PARTIALLY_PAID'
                            ),
                            0
                        ) AS commission_partially_paid,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'PAID'),
                            0
                        ) AS commission_paid,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'CANCELLED'),
                            0
                        ) AS commission_cancelled,
                        COALESCE(
                            SUM(cm.amount) FILTER (WHERE cm.status = 'REVERSED'),
                            0
                        ) AS commission_reversed
                    FROM travel_v2.commissions cm
                    JOIN travel_v2.bookings b ON b.id = cm.booking_id
                    JOIN travel_v2.tracking_links tl
                        ON tl.id = b.tracking_link_id
                    JOIN travel_v2.campaign_applications ca
                        ON ca.id = tl.campaign_application_id
                    JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                    JOIN travel_v2.products p ON p.id = c.product_id
                    WHERE b.merchant_id = :merchantId
                      AND p.merchant_id = :merchantId
                      AND cm.created_at >= COALESCE(
                          CAST(:from AS TIMESTAMPTZ),
                          '-infinity'::TIMESTAMPTZ
                      )
                      AND cm.created_at < COALESCE(
                          CAST(:to AS TIMESTAMPTZ),
                          'infinity'::TIMESTAMPTZ
                      )
                    """ + resellerCampaignFilter(campaignId) + """
                    GROUP BY ca.reseller_id
                )
                SELECT
                    rb.reseller_id,
                    rb.reseller_name,
                    rb.email,
                    rb.campaign_count,
                    rb.application_count,
                    COALESCE(cl.total_clicks, 0) AS total_clicks,
                    COALESCE(bm.booking_total, 0) AS booking_total,
                    COALESCE(bm.booking_pending, 0) AS booking_pending,
                    COALESCE(bm.booking_confirmed, 0) AS booking_confirmed,
                    COALESCE(bm.booking_cancelled, 0) AS booking_cancelled,
                    COALESCE(bm.booking_completed, 0) AS booking_completed,
                    COALESCE(bm.booking_refunded, 0) AS booking_refunded,
                    COALESCE(bm.confirmed_gmv, 0) AS confirmed_gmv,
                    COALESCE(bm.refunded_amount, 0) AS refunded_amount,
                    COALESCE(bm.conversion_pending, 0) AS conversion_pending,
                    COALESCE(bm.conversion_approved, 0) AS conversion_approved,
                    COALESCE(bm.conversion_rejected, 0) AS conversion_rejected,
                    COALESCE(bm.conversion_reversed, 0) AS conversion_reversed,
                    COALESCE(cm.commission_pending, 0) AS commission_pending,
                    COALESCE(cm.commission_approved, 0) AS commission_approved,
                    COALESCE(cm.commission_payable, 0) AS commission_payable,
                    COALESCE(cm.commission_partially_paid, 0)
                        AS commission_partially_paid,
                    COALESCE(cm.commission_paid, 0) AS commission_paid,
                    COALESCE(cm.commission_cancelled, 0) AS commission_cancelled,
                    COALESCE(cm.commission_reversed, 0) AS commission_reversed
                FROM reseller_base rb
                LEFT JOIN clicks cl ON cl.reseller_id = rb.reseller_id
                LEFT JOIN booking_metrics bm ON bm.reseller_id = rb.reseller_id
                LEFT JOIN commission_metrics cm ON cm.reseller_id = rb.reseller_id
                ORDER BY rb.reseller_name, rb.reseller_id
                LIMIT :limit OFFSET :offset
                """;

        return jdbc.query(sql, params, this::mapResellerReport);
    }

    private MapSqlParameterSource campaignParameters(
            UUID merchantId,
            String search,
            String status,
            UUID productId,
            UUID campaignId,
            Instant from,
            Instant to
    ) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("search", search)
                .addValue("status", status)
                .addValue("productId", productId)
                .addValue("campaignId", campaignId)
                .addValue("from", toUtcOffsetDateTime(from))
                .addValue("to", toUtcOffsetDateTime(to));
    }

    private MapSqlParameterSource resellerParameters(
            UUID merchantId,
            String search,
            UUID campaignId,
            Instant from,
            Instant to
    ) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("search", search)
                .addValue("campaignId", campaignId)
                .addValue("from", toUtcOffsetDateTime(from))
                .addValue("to", toUtcOffsetDateTime(to));
    }

    private String campaignWhere(
            String search,
            String status,
            UUID productId,
            UUID campaignId
    ) {
        StringBuilder where = new StringBuilder(
                "WHERE p.merchant_id = :merchantId "
        );

        if (status != null && !status.isBlank()) {
            where.append(" AND c.status = :status ");
        }
        if (productId != null) {
            where.append(" AND p.id = :productId ");
        }
        if (campaignId != null) {
            where.append(" AND c.id = :campaignId ");
        }
        if (search != null && !search.isBlank()) {
            where.append("""
                     AND (
                         POSITION(LOWER(:search) IN LOWER(c.title)) > 0
                         OR POSITION(LOWER(:search) IN LOWER(p.title)) > 0
                     )
                    """);
        }
        return where.toString();
    }

    private String resellerWhere(String search, UUID campaignId) {
        StringBuilder where = new StringBuilder(
                "WHERE p.merchant_id = :merchantId "
        );

        if (campaignId != null) {
            where.append(" AND ca.campaign_id = :campaignId ");
        }
        if (search != null && !search.isBlank()) {
            where.append("""
                     AND (
                         POSITION(LOWER(:search) IN LOWER(u.full_name)) > 0
                         OR POSITION(LOWER(:search) IN LOWER(u.email)) > 0
                     )
                    """);
        }
        return where.toString();
    }

    private String resellerCampaignFilter(UUID campaignId) {
        return campaignId == null
                ? ""
                : " AND ca.campaign_id = :campaignId ";
    }

    private OffsetDateTime toUtcOffsetDateTime(Instant instant) {
        return instant == null
                ? null
                : instant.atOffset(ZoneOffset.UTC);
    }

    private MerchantReportDtos.CampaignReport mapCampaignReport(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new MerchantReportDtos.CampaignReport(
                rs.getObject("campaign_id", UUID.class),
                rs.getString("campaign_title"),
                rs.getString("campaign_status"),
                rs.getObject("product_id", UUID.class),
                rs.getString("product_title"),
                rs.getLong("total_clicks"),
                bookingMetrics(rs),
                conversionMetrics(rs),
                commissionMetrics(rs)
        );
    }

    private MerchantReportDtos.ResellerReport mapResellerReport(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new MerchantReportDtos.ResellerReport(
                rs.getObject("reseller_id", UUID.class),
                rs.getString("reseller_name"),
                rs.getString("email"),
                rs.getLong("campaign_count"),
                rs.getLong("application_count"),
                rs.getLong("total_clicks"),
                bookingMetrics(rs),
                conversionMetrics(rs),
                commissionMetrics(rs)
        );
    }

    private MerchantReportDtos.BookingMetrics bookingMetrics(
            ResultSet rs
    ) throws SQLException {
        return new MerchantReportDtos.BookingMetrics(
                rs.getLong("booking_total"),
                rs.getLong("booking_pending"),
                rs.getLong("booking_confirmed"),
                rs.getLong("booking_cancelled"),
                rs.getLong("booking_completed"),
                rs.getLong("booking_refunded"),
                zeroIfNull(rs.getBigDecimal("confirmed_gmv")),
                zeroIfNull(rs.getBigDecimal("refunded_amount"))
        );
    }

    private MerchantReportDtos.ConversionMetrics conversionMetrics(
            ResultSet rs
    ) throws SQLException {
        return new MerchantReportDtos.ConversionMetrics(
                rs.getLong("conversion_pending"),
                rs.getLong("conversion_approved"),
                rs.getLong("conversion_rejected"),
                rs.getLong("conversion_reversed")
        );
    }

    private MerchantReportDtos.CommissionMetrics commissionMetrics(
            ResultSet rs
    ) throws SQLException {
        return new MerchantReportDtos.CommissionMetrics(
                zeroIfNull(rs.getBigDecimal("commission_pending")),
                zeroIfNull(rs.getBigDecimal("commission_approved")),
                zeroIfNull(rs.getBigDecimal("commission_payable")),
                zeroIfNull(rs.getBigDecimal("commission_partially_paid")),
                zeroIfNull(rs.getBigDecimal("commission_paid")),
                zeroIfNull(rs.getBigDecimal("commission_cancelled")),
                zeroIfNull(rs.getBigDecimal("commission_reversed"))
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}