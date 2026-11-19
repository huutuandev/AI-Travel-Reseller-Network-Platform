package com.aitravel.reseller.repository.merchant;

import com.aitravel.reseller.dto.respone.merchant.ResellerCommissionRankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ResellerCommissionRankingRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public List<ResellerCommissionRankingResponse.Entry> findRankings(UUID merchantId, String currency) {
        return jdbc.query("""
                WITH attributed AS (
                    SELECT commission.reseller_id,
                           SUM(commission.amount) AS total_commission,
                           COUNT(*) AS booking_count
                    FROM travel_v2.commissions commission
                    JOIN travel_v2.bookings booking ON booking.id = commission.booking_id
                    WHERE booking.merchant_id = :merchantId
                      AND commission.currency = :currency
                      AND commission.status IN ('PENDING', 'APPROVED', 'PAYABLE', 'PAID', 'PARTIALLY_PAID')
                      AND booking.booking_status NOT IN ('CANCELLED', 'REFUNDED')
                    GROUP BY commission.reseller_id
                )
                SELECT reseller.id AS reseller_id, user_account.full_name, user_account.email,
                       COALESCE(attributed.total_commission, 0) AS total_commission,
                       COALESCE(attributed.booking_count, 0) AS booking_count
                FROM travel_v2.reseller_profiles reseller
                JOIN travel_v2.users user_account ON user_account.id = reseller.user_id
                LEFT JOIN attributed ON attributed.reseller_id = reseller.id
                WHERE reseller.status = 'ACTIVE'
                  AND (attributed.reseller_id IS NOT NULL OR EXISTS (
                      SELECT 1 FROM travel_v2.campaign_applications application
                      JOIN travel_v2.campaigns campaign ON campaign.id = application.campaign_id
                      JOIN travel_v2.products product ON product.id = campaign.product_id
                      WHERE application.reseller_id = reseller.id
                        AND product.merchant_id = :merchantId
                        AND application.status = 'APPROVED'
                  ))
                ORDER BY total_commission DESC, user_account.full_name ASC, reseller.id ASC
                """, Map.of("currency", currency, "merchantId", merchantId),
                (rs, rowNum) -> new ResellerCommissionRankingResponse.Entry(
                        rowNum + 1L, rs.getObject("reseller_id", java.util.UUID.class),
                        rs.getString("full_name"), rs.getString("email"),
                        rs.getBigDecimal("total_commission"), rs.getLong("booking_count")));
    }
}
