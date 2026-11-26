package com.aitravel.reseller.repository.merchant;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MerchantBookingWebhookRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Optional<BookingState> findForUpdate(
            UUID merchantId,
            UUID bookingId
    ) {
        String sql = """
                SELECT id,
                       tracking_link_id,
                       booking_status
                FROM travel_v2.bookings
                WHERE merchant_id = :merchantId
                  AND id = :bookingId
                FOR UPDATE
                """;

        List<BookingState> result = jdbc.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("bookingId", bookingId),
                (rs, rowNum) -> new BookingState(
                        rs.getObject("id", UUID.class),
                        rs.getObject("tracking_link_id", UUID.class),
                        rs.getString("booking_status")
                )
        );

        return result.stream().findFirst();
    }

    public int transition(
            UUID merchantId,
            UUID bookingId,
            List<String> allowedCurrentStatuses,
            String targetStatus
    ) {
        String sql = """
                UPDATE travel_v2.bookings
                SET booking_status = :targetStatus,
                    updated_at = NOW()
                WHERE merchant_id = :merchantId
                  AND id = :bookingId
                  AND booking_status IN (:allowedStatuses)
                """;

        return jdbc.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("merchantId", merchantId)
                        .addValue("bookingId", bookingId)
                        .addValue("allowedStatuses", allowedCurrentStatuses)
                        .addValue("targetStatus", targetStatus)
        );
    }

    public record BookingState(
            UUID bookingId,
            UUID trackingLinkId,
            String bookingStatus
    ) {
    }
}