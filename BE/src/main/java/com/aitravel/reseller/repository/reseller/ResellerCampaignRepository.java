package com.aitravel.reseller.repository.reseller;

import com.aitravel.reseller.dto.respone.reseller.CampaignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ResellerCampaignRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private static final String SELECT_CAMPAIGN = """
            SELECT
                id,
                title,
                description,
                status,
                commission_type,
                commission_rate,
                commission_currency,
                start_date,
                end_date
            FROM travel_v2.campaigns
            """;

    public List<CampaignResponse> findAllCampaigns() {

        String sql = SELECT_CAMPAIGN + " ORDER BY created_at DESC";

        return jdbc.query(sql, (rs, rowNum) -> new CampaignResponse(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("status"),
                rs.getString("commission_type"),
                rs.getBigDecimal("commission_rate"),
                rs.getString("commission_currency"),
                rs.getTimestamp("start_date") == null
                        ? null
                        : rs.getTimestamp("start_date").toInstant(),
                rs.getTimestamp("end_date") == null
                        ? null
                        : rs.getTimestamp("end_date").toInstant()));
    }

    public Optional<CampaignResponse> findById(UUID campaignId) {

        String sql = SELECT_CAMPAIGN + " WHERE id = :campaignId";

        List<CampaignResponse> result = jdbc.query(
                sql,
                Map.of("campaignId", campaignId),
                (rs, rowNum) -> new CampaignResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getString("commission_type"),
                        rs.getBigDecimal("commission_rate"),
                        rs.getString("commission_currency"),
                        rs.getTimestamp("start_date") == null
                                ? null
                                : rs.getTimestamp("start_date").toInstant(),
                        rs.getTimestamp("end_date") == null
                                ? null
                                : rs.getTimestamp("end_date").toInstant()));

        return result.stream().findFirst();
    }
}