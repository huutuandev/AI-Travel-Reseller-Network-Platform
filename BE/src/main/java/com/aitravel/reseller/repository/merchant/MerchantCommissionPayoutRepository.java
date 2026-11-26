package com.aitravel.reseller.repository.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantFinanceDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MerchantCommissionPayoutRepository {
    private final NamedParameterJdbcTemplate jdbc;

    private static final String COMMISSION_FROM = """
            FROM travel_v2.commissions cm
            JOIN travel_v2.bookings b ON b.id = cm.booking_id
            JOIN travel_v2.reseller_profiles rp ON rp.id = cm.reseller_id
            JOIN travel_v2.users ru ON ru.id = rp.user_id
            JOIN travel_v2.products p ON p.id = b.product_id
            LEFT JOIN travel_v2.tracking_links tl
                ON tl.id = b.tracking_link_id
            LEFT JOIN travel_v2.campaign_applications ca
                ON ca.id = tl.campaign_application_id
            LEFT JOIN travel_v2.campaigns c
                ON c.id = ca.campaign_id
            """;

    public long countCommissions(
            UUID merchantId,
            String status,
            UUID resellerId,
            UUID campaignId,
            UUID bookingId,
            Instant from,
            Instant to
    ) {
        MapSqlParameterSource params = commissionParams(
                merchantId, status, resellerId, campaignId, bookingId, from, to
        );

        String sql = "SELECT COUNT(*) "
                + COMMISSION_FROM
                + commissionWhere(status, resellerId, campaignId, bookingId, from, to);

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<MerchantFinanceDtos.CommissionItem> findCommissionPage(
            UUID merchantId,
            String status,
            UUID resellerId,
            UUID campaignId,
            UUID bookingId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        MapSqlParameterSource params = commissionParams(
                merchantId, status, resellerId, campaignId, bookingId, from, to
        );
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);

        String sql = """
                SELECT
                    cm.id AS commission_id,
                    b.id AS booking_id,
                    b.booking_status,
                    rp.id AS reseller_id,
                    ru.full_name AS reseller_name,
                    c.id AS campaign_id,
                    c.title AS campaign_title,
                    p.id AS product_id,
                    p.title AS product_title,
                    b.total_amount AS booking_amount,
                    b.currency AS booking_currency,
                    cm.amount AS commission_amount,
                    cm.currency AS commission_currency,
                    cm.status AS commission_status,
                    cm.created_at,
                    cm.updated_at
                """ + COMMISSION_FROM
                + commissionWhere(status, resellerId, campaignId, bookingId, from, to)
                + """
                ORDER BY cm.created_at DESC, cm.id
                LIMIT :limit OFFSET :offset
                """;

        return jdbc.query(sql, params, this::mapCommission);
    }

    public Optional<MerchantFinanceDtos.CommissionItem> findCommission(
            UUID merchantId,
            UUID commissionId
    ) {
        String sql = """
                SELECT
                    cm.id AS commission_id,
                    b.id AS booking_id,
                    b.booking_status,
                    rp.id AS reseller_id,
                    ru.full_name AS reseller_name,
                    c.id AS campaign_id,
                    c.title AS campaign_title,
                    p.id AS product_id,
                    p.title AS product_title,
                    b.total_amount AS booking_amount,
                    b.currency AS booking_currency,
                    cm.amount AS commission_amount,
                    cm.currency AS commission_currency,
                    cm.status AS commission_status,
                    cm.created_at,
                    cm.updated_at
                """ + COMMISSION_FROM + """
                WHERE b.merchant_id = :merchantId
                  AND cm.id = :commissionId
                """;

        List<MerchantFinanceDtos.CommissionItem> result = jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("commissionId", commissionId),
                this::mapCommission
        );

        return result.stream().findFirst();
    }

    public Optional<CommissionState> findCommissionStateForUpdate(
            UUID merchantId,
            UUID commissionId
    ) {
        String sql = """
                SELECT
                    cm.id,
                    cm.status,
                    b.booking_status,
                    EXISTS (
                        SELECT 1
                        FROM travel_v2.payout_items pi
                        WHERE pi.commission_id = cm.id
                    ) AS has_payout_item
                FROM travel_v2.commissions cm
                JOIN travel_v2.bookings b ON b.id = cm.booking_id
                WHERE b.merchant_id = :merchantId
                  AND cm.id = :commissionId
                FOR UPDATE OF cm
                """;

        List<CommissionState> rows = jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("commissionId", commissionId),
                (rs, rowNum) -> new CommissionState(
                        rs.getObject("id", UUID.class),
                        rs.getString("status"),
                        rs.getString("booking_status"),
                        rs.getBoolean("has_payout_item")
                )
        );

        return rows.stream().findFirst();
    }

    public int updateCommissionStatus(
            UUID merchantId,
            UUID commissionId,
            String expectedStatus,
            String targetStatus
    ) {
        String sql = """
                UPDATE travel_v2.commissions cm
                SET status = :targetStatus,
                    updated_at = NOW()
                FROM travel_v2.bookings b
                WHERE b.id = cm.booking_id
                  AND b.merchant_id = :merchantId
                  AND cm.id = :commissionId
                  AND cm.status = :expectedStatus
                """;

        return jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("commissionId", commissionId)
                        .addValue("expectedStatus", expectedStatus)
                        .addValue("targetStatus", targetStatus)
        );
    }

    /**
     * Tạo commission idempotent cho booking vừa được duyệt conversion.
     * Currency của FIXED_AMOUNT lấy từ campaign tại thời điểm tạo commission.
     */
    public void createCommissionForApprovedBooking(
            UUID merchantId,
            UUID bookingId
    ) {
        String insertSql = """
                INSERT INTO travel_v2.commissions (
                    booking_id,
                    reseller_id,
                    amount,
                    currency,
                    status
                )
                SELECT
                    b.id,
                    ca.reseller_id,
                    CASE
                        WHEN ca.commission_type_snapshot = 'PERCENTAGE'
                            THEN ROUND(
                                b.total_amount
                                * ca.commission_rate_snapshot
                                / 100,
                                2
                            )
                        WHEN ca.commission_type_snapshot = 'FIXED_AMOUNT'
                            THEN ca.commission_rate_snapshot
                    END,
                    CASE
                        WHEN ca.commission_type_snapshot = 'PERCENTAGE'
                            THEN b.currency
                        WHEN ca.commission_type_snapshot = 'FIXED_AMOUNT'
                            THEN c.commission_currency
                    END,
                    'PENDING'
                FROM travel_v2.bookings b
                JOIN travel_v2.tracking_links tl
                    ON tl.id = b.tracking_link_id
                JOIN travel_v2.campaign_applications ca
                    ON ca.id = tl.campaign_application_id
                JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                WHERE b.id = :bookingId
                  AND b.merchant_id = :merchantId
                  AND b.booking_status = 'CONFIRMED'
                  AND (
                      ca.commission_type_snapshot = 'PERCENTAGE'
                      OR (
                          ca.commission_type_snapshot = 'FIXED_AMOUNT'
                          AND c.commission_currency IS NOT NULL
                      )
                  )
                ON CONFLICT (booking_id) DO NOTHING
                """;

        jdbc.update(
                insertSql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("bookingId", bookingId)
        );

        Integer exists = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM travel_v2.commissions cm
                JOIN travel_v2.bookings b ON b.id = cm.booking_id
                WHERE b.merchant_id = :merchantId
                  AND b.id = :bookingId
                """,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("bookingId", bookingId),
                Integer.class
        );

        if (exists == null || exists != 1) {
            throw new IllegalStateException(
                    "Commission could not be created. Check conversion attribution and fixed commission currency."
            );
        }
    }

    /**
     * Reverse commission khi booking được hoàn tiền.
     * Không sửa Commission nếu nó đã gắn với payout hoặc đã/đang được trả.
     */
    public void reverseCommissionForBooking(
            UUID merchantId,
            UUID bookingId
    ) {
        String sql = """
                SELECT
                    cm.id,
                    cm.status,
                    b.booking_status,
                    EXISTS (
                        SELECT 1
                        FROM travel_v2.payout_items pi
                        WHERE pi.commission_id = cm.id
                    ) AS has_payout_item
                FROM travel_v2.commissions cm
                JOIN travel_v2.bookings b ON b.id = cm.booking_id
                WHERE b.merchant_id = :merchantId
                  AND b.id = :bookingId
                FOR UPDATE OF cm
                """;

        List<CommissionState> rows = jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("bookingId", bookingId),
                (rs, rowNum) -> new CommissionState(
                        rs.getObject("id", UUID.class),
                        rs.getString("status"),
                        rs.getString("booking_status"),
                        rs.getBoolean("has_payout_item")
                )
        );

        if (rows.isEmpty()) {
            return;
        }

        CommissionState commission = rows.getFirst();

        if (commission.hasPayoutItem()
                || "PAID".equals(commission.status())
                || "PARTIALLY_PAID".equals(commission.status())) {
            throw new IllegalStateException(
                    "This commission is already in a payout or has been paid; it cannot be reversed automatically"
            );
        }

        if (!"REVERSED".equals(commission.status())
                && !"CANCELLED".equals(commission.status())) {
            int updated = jdbc.update(
                    """
                    UPDATE travel_v2.commissions
                    SET status = 'REVERSED',
                        updated_at = NOW()
                    WHERE id = :commissionId
                      AND status IN ('PENDING', 'APPROVED', 'PAYABLE')
                    """,
                    new MapSqlParameterSource()
                            .addValue("commissionId", commission.id())
            );

            if (updated != 1) {
                throw new IllegalStateException(
                        "Commission status changed while reversing the conversion"
                );
            }
        }
    }

    public long countPayouts(
            UUID merchantId,
            String status,
            UUID resellerId,
            Instant from,
            Instant to
    ) {
        MapSqlParameterSource params = payoutParams(
                merchantId, status, resellerId, from, to
        );

        String sql = """
                SELECT COUNT(*)
                FROM travel_v2.payouts po
                WHERE po.merchant_id = :merchantId
                """ + payoutWhere(status, resellerId, from, to);

        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<MerchantFinanceDtos.PayoutSummary> findPayoutPage(
            UUID merchantId,
            String status,
            UUID resellerId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        MapSqlParameterSource params = payoutParams(
                merchantId, status, resellerId, from, to
        );
        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);

        String sql = """
                SELECT
                    po.id AS payout_id,
                    rp.id AS reseller_id,
                    u.full_name AS reseller_name,
                    po.total_amount,
                    po.currency,
                    po.payout_status,
                    po.payment_reference,
                    COUNT(pi.id) AS item_count,
                    po.created_at,
                    po.processed_at
                FROM travel_v2.payouts po
                JOIN travel_v2.reseller_profiles rp ON rp.id = po.reseller_id
                JOIN travel_v2.users u ON u.id = rp.user_id
                LEFT JOIN travel_v2.payout_items pi ON pi.payout_id = po.id
                WHERE po.merchant_id = :merchantId
                """ + payoutWhere(status, resellerId, from, to) + """
                GROUP BY
                    po.id, rp.id, u.full_name, po.total_amount, po.currency,
                    po.payout_status, po.payment_reference,
                    po.created_at, po.processed_at
                ORDER BY po.created_at DESC, po.id
                LIMIT :limit OFFSET :offset
                """;

        return jdbc.query(sql, params, this::mapPayoutSummary);
    }

    public Optional<MerchantFinanceDtos.PayoutSummary> findPayoutSummary(
            UUID merchantId,
            UUID payoutId
    ) {
        String sql = """
                SELECT
                    po.id AS payout_id,
                    rp.id AS reseller_id,
                    u.full_name AS reseller_name,
                    po.total_amount,
                    po.currency,
                    po.payout_status,
                    po.payment_reference,
                    COUNT(pi.id) AS item_count,
                    po.created_at,
                    po.processed_at
                FROM travel_v2.payouts po
                JOIN travel_v2.reseller_profiles rp ON rp.id = po.reseller_id
                JOIN travel_v2.users u ON u.id = rp.user_id
                LEFT JOIN travel_v2.payout_items pi ON pi.payout_id = po.id
                WHERE po.merchant_id = :merchantId
                  AND po.id = :payoutId
                GROUP BY
                    po.id, rp.id, u.full_name, po.total_amount, po.currency,
                    po.payout_status, po.payment_reference,
                    po.created_at, po.processed_at
                """;

        List<MerchantFinanceDtos.PayoutSummary> rows = jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("payoutId", payoutId),
                this::mapPayoutSummary
        );

        return rows.stream().findFirst();
    }

    public List<MerchantFinanceDtos.PayoutLine> findPayoutLines(
            UUID merchantId,
            UUID payoutId
    ) {
        String sql = """
                SELECT
                    pi.id AS payout_item_id,
                    cm.id AS commission_id,
                    b.id AS booking_id,
                    c.title AS campaign_title,
                    pi.amount,
                    cm.currency
                FROM travel_v2.payout_items pi
                JOIN travel_v2.payouts po ON po.id = pi.payout_id
                JOIN travel_v2.commissions cm ON cm.id = pi.commission_id
                JOIN travel_v2.bookings b ON b.id = cm.booking_id
                LEFT JOIN travel_v2.tracking_links tl
                    ON tl.id = b.tracking_link_id
                LEFT JOIN travel_v2.campaign_applications ca
                    ON ca.id = tl.campaign_application_id
                LEFT JOIN travel_v2.campaigns c ON c.id = ca.campaign_id
                WHERE po.merchant_id = :merchantId
                  AND po.id = :payoutId
                ORDER BY pi.created_at, pi.id
                """;

        return jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("payoutId", payoutId),
                (rs, rowNum) -> new MerchantFinanceDtos.PayoutLine(
                        rs.getObject("payout_item_id", UUID.class),
                        rs.getObject("commission_id", UUID.class),
                        rs.getObject("booking_id", UUID.class),
                        rs.getString("campaign_title"),
                        rs.getBigDecimal("amount"),
                        rs.getString("currency").trim()
                )
        );
    }

    public List<UUID> lockEligibleCommissionIds(
            UUID merchantId,
            UUID resellerId,
            String currency,
            Collection<UUID> commissionIds
    ) {
        String sql = """
                SELECT cm.id
                FROM travel_v2.commissions cm
                JOIN travel_v2.bookings b ON b.id = cm.booking_id
                WHERE b.merchant_id = :merchantId
                  AND cm.reseller_id = :resellerId
                  AND cm.currency = :currency
                  AND cm.status = 'PAYABLE'
                  AND cm.id IN (:commissionIds)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM travel_v2.payout_items pi
                      WHERE pi.commission_id = cm.id
                  )
                ORDER BY cm.id
                FOR UPDATE OF cm
                """;

        return jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("resellerId", resellerId)
                        .addValue("currency", currency)
                        .addValue("commissionIds", commissionIds),
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );
    }

    public UUID createPayout(
            UUID merchantId,
            UUID resellerId,
            String currency
    ) {
        UUID payoutId = UUID.randomUUID();

        jdbc.update(
                """
                INSERT INTO travel_v2.payouts (
                    id,
                    merchant_id,
                    reseller_id,
                    total_amount,
                    currency,
                    payout_status
                )
                VALUES (
                    :payoutId,
                    :merchantId,
                    :resellerId,
                    0,
                    :currency,
                    'PROCESSING'
                )
                """,
                new MapSqlParameterSource()
                        .addValue("payoutId", payoutId)
                        .addValue("merchantId", merchantId)
                        .addValue("resellerId", resellerId)
                        .addValue("currency", currency)
        );

        return payoutId;
    }

    public void addCommissionToPayout(
            UUID payoutId,
            UUID commissionId
    ) {
        jdbc.update(
                """
                INSERT INTO travel_v2.payout_items (
                    payout_id,
                    commission_id,
                    amount
                )
                SELECT :payoutId, cm.id, cm.amount
                FROM travel_v2.commissions cm
                WHERE cm.id = :commissionId
                """,
                new MapSqlParameterSource()
                        .addValue("payoutId", payoutId)
                        .addValue("commissionId", commissionId)
        );
    }

    public Optional<String> findPayoutStatusForUpdate(
            UUID merchantId,
            UUID payoutId
    ) {
        List<String> statuses = jdbc.query(
                """
                SELECT payout_status
                FROM travel_v2.payouts
                WHERE merchant_id = :merchantId
                  AND id = :payoutId
                FOR UPDATE
                """,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("payoutId", payoutId),
                (rs, rowNum) -> rs.getString("payout_status")
        );

        return statuses.stream().findFirst();
    }

    public long countPayoutItems(UUID merchantId, UUID payoutId) {
        Long count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM travel_v2.payout_items pi
                JOIN travel_v2.payouts po ON po.id = pi.payout_id
                WHERE po.merchant_id = :merchantId
                  AND po.id = :payoutId
                """,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("payoutId", payoutId),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public int markPayoutCommissionsPaid(
            UUID merchantId,
            UUID payoutId
    ) {
        return jdbc.update(
                """
                UPDATE travel_v2.commissions cm
                SET status = 'PAID',
                    updated_at = NOW()
                FROM travel_v2.payout_items pi,
                     travel_v2.payouts po,
                     travel_v2.bookings b
                WHERE pi.commission_id = cm.id
                  AND po.id = pi.payout_id
                  AND b.id = cm.booking_id
                  AND po.id = :payoutId
                  AND po.merchant_id = :merchantId
                  AND b.merchant_id = :merchantId
                  AND cm.status = 'PAYABLE'
                """,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("payoutId", payoutId)
        );
    }

    public int updatePayoutStatus(
            UUID merchantId,
            UUID payoutId,
            String expectedStatus,
            String targetStatus,
            String paymentReference
    ) {
        return jdbc.update(
                """
                UPDATE travel_v2.payouts
                SET payout_status = :targetStatus,
                    payment_reference = CASE
                        WHEN :targetStatus = 'PROCESSING' THEN NULL
                        ELSE COALESCE(:paymentReference, payment_reference)
                    END,
                    processed_at = CASE
                        WHEN :targetStatus IN ('COMPLETED', 'FAILED')
                            THEN NOW()
                        ELSE NULL
                    END
                WHERE merchant_id = :merchantId
                  AND id = :payoutId
                  AND payout_status = :expectedStatus
                """,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("payoutId", payoutId)
                        .addValue("expectedStatus", expectedStatus)
                        .addValue("targetStatus", targetStatus)
                        .addValue("paymentReference", paymentReference)
        );
    }

    private MapSqlParameterSource commissionParams(
            UUID merchantId,
            String status,
            UUID resellerId,
            UUID campaignId,
            UUID bookingId,
            Instant from,
            Instant to
    ) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("status", status)
                .addValue("resellerId", resellerId)
                .addValue("campaignId", campaignId)
                .addValue("bookingId", bookingId)
                .addValue("from", utc(from))
                .addValue("to", utc(to));
    }

    private String commissionWhere(
            String status,
            UUID resellerId,
            UUID campaignId,
            UUID bookingId,
            Instant from,
            Instant to
    ) {
        StringBuilder where = new StringBuilder(
                " WHERE b.merchant_id = :merchantId "
        );

        if (status != null && !status.isBlank()) {
            where.append(" AND cm.status = :status ");
        }
        if (resellerId != null) {
            where.append(" AND cm.reseller_id = :resellerId ");
        }
        if (campaignId != null) {
            where.append(" AND c.id = :campaignId ");
        }
        if (bookingId != null) {
            where.append(" AND b.id = :bookingId ");
        }
        if (from != null) {
            where.append(" AND cm.created_at >= CAST(:from AS TIMESTAMPTZ) ");
        }
        if (to != null) {
            where.append(" AND cm.created_at < CAST(:to AS TIMESTAMPTZ) ");
        }

        return where.toString();
    }

    private MapSqlParameterSource payoutParams(
            UUID merchantId,
            String status,
            UUID resellerId,
            Instant from,
            Instant to
    ) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("status", status)
                .addValue("resellerId", resellerId)
                .addValue("from", utc(from))
                .addValue("to", utc(to));
    }

    private String payoutWhere(
            String status,
            UUID resellerId,
            Instant from,
            Instant to
    ) {
        StringBuilder where = new StringBuilder();

        if (status != null && !status.isBlank()) {
            where.append(" AND po.payout_status = :status ");
        }
        if (resellerId != null) {
            where.append(" AND po.reseller_id = :resellerId ");
        }
        if (from != null) {
            where.append(" AND po.created_at >= CAST(:from AS TIMESTAMPTZ) ");
        }
        if (to != null) {
            where.append(" AND po.created_at < CAST(:to AS TIMESTAMPTZ) ");
        }

        return where.toString();
    }

    private OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private MerchantFinanceDtos.CommissionItem mapCommission(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new MerchantFinanceDtos.CommissionItem(
                rs.getObject("commission_id", UUID.class),
                rs.getObject("booking_id", UUID.class),
                rs.getString("booking_status"),
                rs.getObject("reseller_id", UUID.class),
                rs.getString("reseller_name"),
                rs.getObject("campaign_id", UUID.class),
                rs.getString("campaign_title"),
                rs.getObject("product_id", UUID.class),
                rs.getString("product_title"),
                rs.getBigDecimal("booking_amount"),
                rs.getString("booking_currency").trim(),
                rs.getBigDecimal("commission_amount"),
                rs.getString("commission_currency").trim(),
                rs.getString("commission_status"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private MerchantFinanceDtos.PayoutSummary mapPayoutSummary(
            ResultSet rs,
            int rowNum
    ) throws SQLException {
        return new MerchantFinanceDtos.PayoutSummary(
                rs.getObject("payout_id", UUID.class),
                rs.getObject("reseller_id", UUID.class),
                rs.getString("reseller_name"),
                rs.getBigDecimal("total_amount"),
                rs.getString("currency").trim(),
                rs.getString("payout_status"),
                rs.getString("payment_reference"),
                rs.getLong("item_count"),
                instant(rs, "created_at"),
                instant(rs, "processed_at")
        );
    }

    private Instant instant(ResultSet rs, String column)
            throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record CommissionState(
            UUID id,
            String status,
            String bookingStatus,
            boolean hasPayoutItem
    ) {}
}