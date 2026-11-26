package com.aitravel.reseller.dto.respone.merchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public final class MerchantBookingDtos {
    private MerchantBookingDtos() {}

    public record PageResponse(
            List<BookingItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record BookingItem(
            UUID bookingId,
            UUID customerId,
            String customerName,
            String customerEmail,
            UUID productId,
            String productTitle,
            UUID trackingLinkId,
            String trackingCode,
            UUID campaignId,
            String campaignTitle,
            UUID resellerId,
            String resellerName,
            BigDecimal totalAmount,
            String currency,
            String bookingStatus,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ConversionPageResponse(
            List<ConversionItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record ConversionItem(
            BookingItem booking,
            String conversionStatus
    ) {}

    public record ConversionReviewRequest(
                @NotBlank String decision
    ) {}

    public record ConversionReviewResponse(
                UUID bookingId,
                String conversionStatus,
                String bookingStatus,
                Instant updatedAt,
                boolean commissionVerified
        ) {}
}