package com.aitravel.reseller.dto.request.merchant;

import java.time.Instant;
import java.util.UUID;

public final class MerchantBookingWebhookDtos {

    private MerchantBookingWebhookDtos() {
    }

    public enum Event {
        PAYMENT_CONFIRMED,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED
    }

    public record Request(
        UUID bookingId,
        Event event
    ) {
    }

    public record Result(
        UUID bookingId,
        Event event,
        String bookingStatus,
        boolean stateChanged,
        Instant processedAt
    ) {
    }

    public record Credential(
        String merchantCode,
        String signingSecret,
        String signatureAlgorithm,
        String signedContentFormat,
        String signatureHeaderFormat
    ) {
    }
}