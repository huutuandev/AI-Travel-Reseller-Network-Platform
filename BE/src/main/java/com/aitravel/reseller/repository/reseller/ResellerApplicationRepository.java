package com.aitravel.reseller.repository.reseller;

import com.aitravel.reseller.dto.respone.reseller.ApplicationResponse;
import com.aitravel.reseller.dto.respone.reseller.RegisteredCampaignResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ResellerApplicationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public boolean isCampaignEligible(UUID campaignId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM travel_v2.campaigns c
                    JOIN travel_v2.products p ON p.id = c.product_id
                    JOIN travel_v2.merchants m ON m.id = p.merchant_id
                    JOIN travel_v2.users u ON u.id = m.user_id
                    WHERE c.id = :campaignId
                      AND c.status = 'ACTIVE'
                      AND (c.end_date IS NULL OR c.end_date > CURRENT_TIMESTAMP)
                      AND p.availability_status = 'AVAILABLE'
                      AND m.status = 'ACTIVE'
                      AND u.status = 'ACTIVE'
                      AND (
                          (c.commission_type = 'PERCENTAGE'
                           AND c.commission_currency IS NULL)
                          OR
                          (c.commission_type = 'FIXED_AMOUNT'
                           AND c.commission_currency ~ '^[A-Z]{3}$')
                      )
                )
                """;

        Boolean result = jdbc.queryForObject(
                sql,
                Map.of("campaignId", campaignId),
                Boolean.class);

        return Boolean.TRUE.equals(result);
    }

    public Optional<ApplicationResponse> create(
            UUID campaignId,
            UUID resellerId) {
        String sql = """
                INSERT INTO travel_v2.campaign_applications
                    (id, campaign_id, reseller_id, status, applied_at)
                VALUES
                    (:id, :campaignId, :resellerId, 'PENDING', :appliedAt)
                ON CONFLICT (campaign_id, reseller_id) DO NOTHING
                RETURNING id, campaign_id, status, applied_at
                """;

        Map<String, Object> params = Map.of(
                "id", UUID.randomUUID(),
                "campaignId", campaignId,
                "resellerId", resellerId,
                "appliedAt", java.sql.Timestamp.from(Instant.now()));

        List<ApplicationResponse> result = jdbc.query(
                sql,
                params,
                (rs, rowNum) -> new ApplicationResponse(
                        rs.getObject("id", UUID.class),
                        rs.getObject("campaign_id", UUID.class),
                        rs.getString("status"),
                        rs.getTimestamp("applied_at").toInstant()));

        return result.stream().findFirst();
    }

    public List<RegisteredCampaignResponse> findAllByResellerId(UUID resellerId) {

        String sql = """
                SELECT
                    a.id AS application_id,
                    c.id AS campaign_id,
                    c.title,
                    c.description,
                    c.status AS campaign_status,
                    a.status AS application_status,
                    c.commission_type,
                    c.commission_rate,
                    a.applied_at
                FROM travel_v2.campaign_applications a
                JOIN travel_v2.campaigns c
                    ON c.id = a.campaign_id
                WHERE a.reseller_id = :resellerId
                ORDER BY a.applied_at DESC
                """;

        return jdbc.query(
                sql,
                Map.of("resellerId", resellerId),
                (rs, rowNum) -> new RegisteredCampaignResponse(
                        rs.getObject("application_id", UUID.class),
                        rs.getObject("campaign_id", UUID.class),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("campaign_status"),
                        rs.getString("application_status"),
                        rs.getString("commission_type"),
                        rs.getBigDecimal("commission_rate"),
                        rs.getTimestamp("applied_at") == null
                                ? null
                                : rs.getTimestamp("applied_at").toInstant()));
    }
}