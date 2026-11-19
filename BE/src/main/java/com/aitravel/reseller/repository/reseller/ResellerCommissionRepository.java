package com.aitravel.reseller.repository.reseller;

import com.aitravel.reseller.dto.respone.reseller.CommissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ResellerCommissionRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<CommissionResponse> findAllByResellerId(UUID resellerId) {

        String sql = """
                SELECT
                    c.title AS campaign,
                    b.total_amount AS order_amount,
                    cm.amount AS commission_amount,
                    cm.currency,
                    cm.status,
                    cm.created_at
                FROM travel_v2.commissions cm
                JOIN travel_v2.bookings b ON b.id = cm.booking_id
                LEFT JOIN travel_v2.tracking_links tl ON tl.id = b.tracking_link_id
                LEFT JOIN travel_v2.campaign_applications a ON a.id = tl.campaign_application_id
                LEFT JOIN travel_v2.campaigns c ON c.id = a.campaign_id
                WHERE cm.reseller_id = :resellerId
                ORDER BY cm.created_at DESC
                """;

        return jdbc.query(
                sql,
                Map.of("resellerId", resellerId),
                (rs, rowNum) -> new CommissionResponse(
                        rs.getString("campaign"),
                        rs.getBigDecimal("order_amount"),
                        rs.getBigDecimal("commission_amount"),
                        rs.getString("currency"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at") == null
                                ? null
                                : rs.getTimestamp("created_at").toInstant()));
    }
}
