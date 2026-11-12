package com.aitravel.reseller.repository.reseller;

import com.aitravel.reseller.dto.respone.reseller.TrackingLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ResellerTrackingLinkRepository {

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${app.tracking.base-url}")
    private String trackingBaseUrl;

    public List<TrackingLinkResponse> findAllByResellerId(UUID resellerId) {

        String sql = """
                SELECT
                    c.title AS campaign,
                    tl.tracking_code,
                    tl.created_at,
                    COALESCE(cl.clicks, 0) AS clicks,
                    COALESCE(cv.conversions, 0) AS conversions
                FROM travel_v2.tracking_links tl
                JOIN travel_v2.campaigns c
                    ON c.id = tl.campaign_id
                LEFT JOIN (
                    SELECT tracking_link_id, COUNT(*) AS clicks
                    FROM travel_v2.tracking_clicks
                    GROUP BY tracking_link_id
                ) cl ON cl.tracking_link_id = tl.id
                LEFT JOIN (
                    SELECT tracking_link_id, COUNT(*) AS conversions
                    FROM travel_v2.conversions
                    GROUP BY tracking_link_id
                ) cv ON cv.tracking_link_id = tl.id
                WHERE tl.reseller_id = :resellerId
                ORDER BY tl.created_at DESC
                """;

        return jdbc.query(
                sql,
                Map.of("resellerId", resellerId),
                (rs, rowNum) -> new TrackingLinkResponse(
                        rs.getString("campaign"),
                        trackingBaseUrl.replaceAll("/+$", "")
                                + "/r/" + rs.getString("tracking_code"),
                        rs.getLong("clicks"),
                        rs.getLong("conversions"),
                        rs.getTimestamp("created_at").toInstant()));
    }
}