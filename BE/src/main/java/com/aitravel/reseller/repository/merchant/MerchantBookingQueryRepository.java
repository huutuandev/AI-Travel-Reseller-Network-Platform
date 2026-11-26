package com.aitravel.reseller.repository.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantBookingDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

@Repository
@RequiredArgsConstructor
public class MerchantBookingQueryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    private static final String FROM_AND_JOINS = """
            FROM travel_v2.bookings b
            JOIN travel_v2.products p ON p.id = b.product_id
            LEFT JOIN travel_v2.users cu ON cu.id = b.customer_id
            LEFT JOIN travel_v2.tracking_links tl ON tl.id = b.tracking_link_id
            LEFT JOIN travel_v2.campaign_applications ca
                ON ca.id = tl.campaign_application_id
            LEFT JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
            LEFT JOIN travel_v2.reseller_profiles rp ON rp.id = tl.reseller_id
            LEFT JOIN travel_v2.users ru ON ru.id = rp.user_id
            """;

    public long countForMerchant(
            UUID merchantId,
            String search,
            String status,
            UUID productId,
            UUID resellerId,
            Instant from,
            Instant to
    ) {
        MapSqlParameterSource params = parameters(
                merchantId, search, status, productId, resellerId, from, to
        );

        String sql = """
                SELECT COUNT(*)
                """ + FROM_AND_JOINS + buildWhere(params, search, status,
                productId, resellerId, from, to);

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<MerchantBookingDtos.BookingItem> findPageForMerchant(
            UUID merchantId,
            String search,
            String status,
            UUID productId,
            UUID resellerId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        MapSqlParameterSource params = parameters(
                merchantId, search, status, productId, resellerId, from, to
        );
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);

        String sql = """
                SELECT
                    b.id AS booking_id,
                    b.customer_id,
                    cu.full_name AS customer_name,
                    cu.email AS customer_email,
                    b.product_id,
                    p.title AS product_title,
                    b.tracking_link_id,
                    tl.tracking_code,
                    c.id AS campaign_id,
                    c.title AS campaign_title,
                    rp.id AS reseller_id,
                    ru.full_name AS reseller_name,
                    b.total_amount,
                    b.currency,
                    b.booking_status,
                    b.created_at,
                    b.updated_at
                """ + FROM_AND_JOINS + buildWhere(params, search, status,
                productId, resellerId, from, to) + """
                ORDER BY b.created_at DESC, b.id
                LIMIT :limit OFFSET :offset
                """;

        return jdbc.query(sql, params, this::mapBooking);
    }

    public Optional<MerchantBookingDtos.BookingItem> findDetailForMerchant(
            UUID merchantId,
            UUID bookingId
    ) {
        String sql = """
                SELECT
                    b.id AS booking_id,
                    b.customer_id,
                    cu.full_name AS customer_name,
                    cu.email AS customer_email,
                    b.product_id,
                    p.title AS product_title,
                    b.tracking_link_id,
                    tl.tracking_code,
                    c.id AS campaign_id,
                    c.title AS campaign_title,
                    rp.id AS reseller_id,
                    ru.full_name AS reseller_name,
                    b.total_amount,
                    b.currency,
                    b.booking_status,
                    b.created_at,
                    b.updated_at
                """ + FROM_AND_JOINS + """
                WHERE b.merchant_id = :merchantId
                  AND b.id = :bookingId
                """;

        List<MerchantBookingDtos.BookingItem> results = jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("bookingId", bookingId),
                this::mapBooking
        );

        return results.stream().findFirst();
    }

    private MapSqlParameterSource parameters(
            UUID merchantId,
            String search,
            String status,
            UUID productId,
            UUID resellerId,
            Instant from,
            Instant to
    ) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("search", search)
                .addValue("status", status)
                .addValue("productId", productId)
                .addValue("resellerId", resellerId)
                .addValue("from", from)
                .addValue("to", to);
    }

    private String buildWhere(
            MapSqlParameterSource params,
            String search,
            String status,
            UUID productId,
            UUID resellerId,
            Instant from,
            Instant to
    ) {
        StringBuilder where = new StringBuilder(
                " WHERE b.merchant_id = :merchantId "
        );

        if (status != null && !status.isBlank()) {
            where.append(" AND b.booking_status = :status ");
        }

        if (productId != null) {
            where.append(" AND b.product_id = :productId ");
        }

        if (resellerId != null) {
            where.append(" AND rp.id = :resellerId ");
        }

        if (from != null) {
            where.append(" AND b.created_at >= :from ");
        }

        if (to != null) {
            where.append(" AND b.created_at < :to ");
        }

        if (search != null && !search.isBlank()) {
            where.append("""
                     AND (
                         POSITION(LOWER(:search) IN LOWER(COALESCE(p.title, ''))) > 0
                         OR POSITION(LOWER(:search) IN LOWER(COALESCE(cu.full_name, ''))) > 0
                         OR POSITION(LOWER(:search) IN LOWER(COALESCE(cu.email, ''))) > 0
                         OR POSITION(LOWER(:search) IN LOWER(COALESCE(ru.full_name, ''))) > 0
                         OR POSITION(LOWER(:search) IN LOWER(COALESCE(tl.tracking_code, ''))) > 0
                     )
                    """);
        }

        return where.toString();
    }

    private MerchantBookingDtos.BookingItem mapBooking(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new MerchantBookingDtos.BookingItem(
                rs.getObject("booking_id", UUID.class),
                rs.getObject("customer_id", UUID.class),
                rs.getString("customer_name"),
                rs.getString("customer_email"),
                rs.getObject("product_id", UUID.class),
                rs.getString("product_title"),
                rs.getObject("tracking_link_id", UUID.class),
                rs.getString("tracking_code"),
                rs.getObject("campaign_id", UUID.class),
                rs.getString("campaign_title"),
                rs.getObject("reseller_id", UUID.class),
                rs.getString("reseller_name"),
                rs.getBigDecimal("total_amount"),
                rs.getString("currency").trim(),
                rs.getString("booking_status"),
                toInstant(rs, "created_at"),
                toInstant(rs, "updated_at")
        );
    }

    private Instant toInstant(ResultSet rs, String column)
            throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    public long countConversionsForMerchant(
            UUID merchantId,
            Collection<String> bookingStatuses
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("bookingStatuses", bookingStatuses);

        String sql = """
                SELECT COUNT(*)
                """ + FROM_AND_JOINS + """
                WHERE b.merchant_id = :merchantId
                AND b.tracking_link_id IS NOT NULL
                AND b.booking_status IN (:bookingStatuses)
                """;

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<MerchantBookingDtos.BookingItem> findConversionPageForMerchant(
            UUID merchantId,
            Collection<String> bookingStatuses,
            int page,
            int size
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("bookingStatuses", bookingStatuses)
                .addValue("limit", size)
                .addValue("offset", (long) page * size);

        String sql = """
                SELECT
                    b.id AS booking_id,
                    b.customer_id,
                    cu.full_name AS customer_name,
                    cu.email AS customer_email,
                    b.product_id,
                    p.title AS product_title,
                    b.tracking_link_id,
                    tl.tracking_code,
                    c.id AS campaign_id,
                    c.title AS campaign_title,
                    rp.id AS reseller_id,
                    ru.full_name AS reseller_name,
                    b.total_amount,
                    b.currency,
                    b.booking_status,
                    b.created_at,
                    b.updated_at
                """ + FROM_AND_JOINS + """
                WHERE b.merchant_id = :merchantId
                AND b.tracking_link_id IS NOT NULL
                AND b.booking_status IN (:bookingStatuses)
                ORDER BY b.created_at DESC, b.id
                LIMIT :limit OFFSET :offset
                """;

        return jdbc.query(sql, params, this::mapBooking);
    }

    public int transitionConversionBooking(
            UUID merchantId,
            UUID bookingId,
            Collection<String> allowedCurrentStatuses,
            String targetBookingStatus
    ) {
        String sql = """
                UPDATE travel_v2.bookings
                SET booking_status = :targetBookingStatus,
                    updated_at = NOW()
                WHERE merchant_id = :merchantId
                AND id = :bookingId
                AND tracking_link_id IS NOT NULL
                AND booking_status IN (:allowedCurrentStatuses)
                """;

        return jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("bookingId", bookingId)
                        .addValue("allowedCurrentStatuses", allowedCurrentStatuses)
                        .addValue("targetBookingStatus", targetBookingStatus)
        );
    }
}