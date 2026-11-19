package com.aitravel.reseller.repository.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantResellerDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class MerchantResellerRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public long countResellers(
            UUID merchantId,
            String search,
            UUID campaignId,
            String applicationStatus
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT rp.id)
                FROM travel_v2.reseller_profiles rp
                JOIN travel_v2.users u ON u.id = rp.user_id
                JOIN travel_v2.campaign_applications a ON a.reseller_id = rp.id
                JOIN travel_v2.campaigns c ON c.id = a.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                WHERE p.merchant_id = :merchantId
                """);

        MapSqlParameterSource params = baseParams(
                merchantId, search, campaignId, applicationStatus
        );
        appendFilters(sql, params, search, campaignId, applicationStatus);

        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count == null ? 0L : count;
    }

    public List<MerchantResellerDtos.ResellerSummary> findResellerPage(
            UUID merchantId,
            String search,
            UUID campaignId,
            String applicationStatus,
            int page,
            int size
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    rp.id AS reseller_id,
                    u.full_name,
                    u.email,
                    u.phone,
                    rp.status AS reseller_profile_status,
                    rp.created_at AS reseller_since,
                    COUNT(DISTINCT a.id) AS campaign_count
                FROM travel_v2.reseller_profiles rp
                JOIN travel_v2.users u ON u.id = rp.user_id
                JOIN travel_v2.campaign_applications a ON a.reseller_id = rp.id
                JOIN travel_v2.campaigns c ON c.id = a.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                WHERE p.merchant_id = :merchantId
                """);

        MapSqlParameterSource params = baseParams(
                merchantId, search, campaignId, applicationStatus
        );
        appendFilters(sql, params, search, campaignId, applicationStatus);

        sql.append("""
                GROUP BY rp.id, u.full_name, u.email, u.phone, rp.status, rp.created_at
                ORDER BY MAX(a.applied_at) DESC, rp.id
                LIMIT :limit OFFSET :offset
                """);

        params.addValue("limit", size);
        params.addValue("offset", (long) page * size);

        List<ResellerRow> rows = jdbc.query(
                sql.toString(),
                params,
                (rs, rowNum) -> new ResellerRow(
                        rs.getObject("reseller_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("reseller_profile_status"),
                        rs.getTimestamp("reseller_since").toInstant(),
                        rs.getLong("campaign_count")
                )
        );

        if (rows.isEmpty()) {
            return List.of();
        }

        List<UUID> resellerIds = rows.stream()
                .map(ResellerRow::resellerId)
                .toList();

        Map<UUID, List<MerchantResellerDtos.CampaignParticipation>> applications =
                new HashMap<>();

        for (MerchantResellerDtos.CampaignParticipation application :
                findApplications(
                        merchantId,
                        resellerIds,
                        campaignId,
                        applicationStatus
                )) {
            applications.computeIfAbsent(
                    application.resellerId(),
                    ignored -> new ArrayList<>()
            ).add(application);
        }

        return rows.stream()
                .map(row -> new MerchantResellerDtos.ResellerSummary(
                        row.resellerId(),
                        row.fullName(),
                        row.email(),
                        row.phone(),
                        row.resellerProfileStatus(),
                        row.resellerSince(),
                        row.campaignCount(),
                        applications.getOrDefault(row.resellerId(), List.of())
                ))
                .toList();
    }

    public Optional<MerchantResellerDtos.ResellerIdentity> findReseller(
            UUID merchantId,
            UUID resellerId
    ) {
        String sql = """
                SELECT
                    rp.id AS reseller_id,
                    u.full_name,
                    u.email,
                    u.phone,
                    rp.status AS reseller_profile_status,
                    rp.created_at AS reseller_since
                FROM travel_v2.reseller_profiles rp
                JOIN travel_v2.users u ON u.id = rp.user_id
                WHERE rp.id = :resellerId
                  AND EXISTS (
                      SELECT 1
                      FROM travel_v2.campaign_applications a
                      JOIN travel_v2.campaigns c ON c.id = a.campaign_id
                      JOIN travel_v2.products p ON p.id = c.product_id
                      WHERE a.reseller_id = rp.id
                        AND p.merchant_id = :merchantId
                  )
                """;

        List<ResellerRow> rows = jdbc.query(
                sql,
                Map.of("merchantId", merchantId, "resellerId", resellerId),
                (rs, rowNum) -> new ResellerRow(
                        rs.getObject("reseller_id", UUID.class),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("reseller_profile_status"),
                        rs.getTimestamp("reseller_since").toInstant(),
                        0
                )
        );

        return rows.stream()
                .findFirst()
                .map(row -> new MerchantResellerDtos.ResellerIdentity(
                        row.resellerId(),
                        row.fullName(),
                        row.email(),
                        row.phone(),
                        row.resellerProfileStatus(),
                        row.resellerSince()
                ));
    }

    public List<MerchantResellerDtos.CampaignParticipation> findApplications(
            UUID merchantId,
            List<UUID> resellerIds,
            UUID campaignId,
            String applicationStatus
    ) {
        if (resellerIds == null || resellerIds.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.id AS application_id,
                    a.reseller_id,
                    c.id AS campaign_id,
                    c.title AS campaign_title,
                    p.id AS product_id,
                    p.title AS product_title,
                    c.status AS campaign_status,
                    a.status AS application_status,
                    a.commission_type_snapshot AS commission_type,
                    a.commission_rate_snapshot AS commission_rate,
                    a.applied_at,
                    a.reviewed_at
                FROM travel_v2.campaign_applications a
                JOIN travel_v2.campaigns c ON c.id = a.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                WHERE p.merchant_id = :merchantId
                  AND a.reseller_id IN (:resellerIds)
                """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("resellerIds", resellerIds);

        if (campaignId != null) {
            sql.append(" AND c.id = :campaignId ");
            params.addValue("campaignId", campaignId);
        }

        if (applicationStatus != null && !applicationStatus.isBlank()) {
            sql.append(" AND a.status = :applicationStatus ");
            params.addValue("applicationStatus", applicationStatus);
        }

        sql.append(" ORDER BY a.applied_at DESC, a.id ");

        return jdbc.query(
                sql.toString(),
                params,
                (rs, rowNum) -> mapParticipation(rs)
        );
    }

    public Optional<String> findStatusForUpdate(
            UUID merchantId,
            UUID applicationId
    ) {
        String sql = """
                SELECT a.status
                FROM travel_v2.campaign_applications a
                JOIN travel_v2.campaigns c ON c.id = a.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                WHERE a.id = :applicationId
                  AND p.merchant_id = :merchantId
                FOR UPDATE OF a
                """;

        List<String> statuses = jdbc.query(
                sql,
                Map.of(
                        "applicationId", applicationId,
                        "merchantId", merchantId
                ),
                (rs, rowNum) -> rs.getString("status")
        );

        return statuses.stream().findFirst();
    }

    public int updateApplicationStatus(
            UUID applicationId,
            String status
    ) {
        String sql = """
                UPDATE travel_v2.campaign_applications
                SET status = :status,
                    reviewed_at = CURRENT_TIMESTAMP
                WHERE id = :applicationId
                """;

        return jdbc.update(
                sql,
                Map.of(
                        "applicationId", applicationId,
                        "status", status
                )
        );
    }

    public Optional<MerchantResellerDtos.CampaignParticipation>
    findApplicationById(UUID merchantId, UUID applicationId) {
        String sql = """
                SELECT
                    a.id AS application_id,
                    a.reseller_id,
                    c.id AS campaign_id,
                    c.title AS campaign_title,
                    p.id AS product_id,
                    p.title AS product_title,
                    c.status AS campaign_status,
                    a.status AS application_status,
                    a.commission_type_snapshot AS commission_type,
                    a.commission_rate_snapshot AS commission_rate,
                    a.applied_at,
                    a.reviewed_at
                FROM travel_v2.campaign_applications a
                JOIN travel_v2.campaigns c ON c.id = a.campaign_id
                JOIN travel_v2.products p ON p.id = c.product_id
                WHERE a.id = :applicationId
                  AND p.merchant_id = :merchantId
                """;

        List<MerchantResellerDtos.CampaignParticipation> rows = jdbc.query(
                sql,
                Map.of(
                        "applicationId", applicationId,
                        "merchantId", merchantId
                ),
                (rs, rowNum) -> mapParticipation(rs)
        );

        return rows.stream().findFirst();
    }

    private MapSqlParameterSource baseParams(
            UUID merchantId,
            String search,
            UUID campaignId,
            String applicationStatus
    ) {
        return new MapSqlParameterSource()
                .addValue("merchantId", merchantId)
                .addValue("search", search == null ? "" : search.toLowerCase(Locale.ROOT))
                .addValue("campaignId", campaignId)
                .addValue("applicationStatus", applicationStatus);
    }

    private void appendFilters(
            StringBuilder sql,
            MapSqlParameterSource params,
            String search,
            UUID campaignId,
            String applicationStatus
    ) {
        if (search != null && !search.isBlank()) {
            sql.append("""
                     AND (
                         POSITION(:search IN LOWER(u.full_name)) > 0
                         OR POSITION(:search IN LOWER(u.email)) > 0
                         OR POSITION(:search IN LOWER(COALESCE(u.phone, ''))) > 0
                     )
                    """);
            params.addValue("search", search.toLowerCase(Locale.ROOT));
        }

        if (campaignId != null) {
            sql.append(" AND c.id = :campaignId ");
            params.addValue("campaignId", campaignId);
        }

        if (applicationStatus != null && !applicationStatus.isBlank()) {
            sql.append(" AND a.status = :applicationStatus ");
            params.addValue("applicationStatus", applicationStatus);
        }
    }

    private MerchantResellerDtos.CampaignParticipation mapParticipation(
            ResultSet rs
    ) throws SQLException {
        var appliedAt = rs.getTimestamp("applied_at");
        var reviewedAt = rs.getTimestamp("reviewed_at");

        return new MerchantResellerDtos.CampaignParticipation(
                rs.getObject("application_id", UUID.class),
                rs.getObject("reseller_id", UUID.class),
                rs.getObject("campaign_id", UUID.class),
                rs.getString("campaign_title"),
                rs.getObject("product_id", UUID.class),
                rs.getString("product_title"),
                rs.getString("campaign_status"),
                rs.getString("application_status"),
                rs.getString("commission_type"),
                rs.getBigDecimal("commission_rate"),
                appliedAt == null ? null : appliedAt.toInstant(),
                reviewedAt == null ? null : reviewedAt.toInstant()
        );
    }

    private record ResellerRow(
            UUID resellerId,
            String fullName,
            String email,
            String phone,
            String resellerProfileStatus,
            java.time.Instant resellerSince,
            long campaignCount
    ) {}
}