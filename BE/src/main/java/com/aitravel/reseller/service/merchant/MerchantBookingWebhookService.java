package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.MerchantBookingWebhookDtos;
import com.aitravel.reseller.dto.request.merchant.MerchantBookingWebhookDtos.Event;
import com.aitravel.reseller.dto.request.merchant.MerchantBookingWebhookDtos.Result;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.MerchantBookingWebhookRepository;
import com.aitravel.reseller.repository.merchant.MerchantBookingWebhookRepository.BookingState;
import com.aitravel.reseller.repository.merchant.MerchantCommissionPayoutRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantBookingWebhookService {

    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);
    private static final int MAX_BODY_LENGTH = 65_536;

    private final MerchantRepository merchantRepository;
    private final MerchantBookingWebhookRepository bookingRepository;
    private final MerchantCommissionPayoutRepository financeRepository;
    private final Environment environment;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Result process(
            String merchantCodeHeader,
            String timestampHeader,
            String signatureHeader,
            String rawBody
    ) {
        String merchantCode = normalizeMerchantCode(merchantCodeHeader);
        long timestamp = parseTimestamp(timestampHeader);

        if (rawBody == null || rawBody.isBlank()
                || rawBody.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException(
                    "Webhook body must be present and no larger than 64 KB"
            );
        }

        Merchant merchant = merchantRepository.findByCode(merchantCode)
                .orElseThrow(this::invalidCredentials);

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw invalidCredentials();
        }

        String secret = deriveSigningSecret(merchant.getId());

        verifySignature(
                secret,
                timestamp,
                signatureHeader,
                rawBody
        );

        MerchantBookingWebhookDtos.Request request = parseRequest(rawBody);

        BookingState booking = bookingRepository.findForUpdate(
                        merchant.getId(),
                        request.bookingId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found for this merchant"
                ));

        String currentStatus = booking.bookingStatus();
        String targetStatus;
        List<String> allowedStatuses;

        switch (request.event()) {
            case PAYMENT_CONFIRMED -> {
                targetStatus = "CONFIRMED";
                allowedStatuses = List.of("PENDING");
            }

            case PAYMENT_FAILED -> {
                // Thanh toán thất bại có thể chỉ là tạm thời.
                // Ghi nhận request thành công nhưng không hủy booking.
                return new Result(
                        booking.bookingId(),
                        request.event(),
                        currentStatus,
                        false,
                        Instant.now()
                );
            }

            case PAYMENT_REFUNDED -> {
                targetStatus = "REFUNDED";
                allowedStatuses = List.of("CONFIRMED", "COMPLETED");
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported webhook event"
            );
        }

        // Cùng event được gửi lại: trả thành công nhưng không lặp side effect.
        if (targetStatus.equals(currentStatus)
                || (request.event() == Event.PAYMENT_CONFIRMED
                && "COMPLETED".equals(currentStatus))) {
            return new Result(
                    booking.bookingId(),
                    request.event(),
                    targetStatus,
                    true,
                    Instant.now()
            );
        }

        if (!allowedStatuses.contains(currentStatus)) {
            throw new IllegalStateException(
                    "Event " + request.event()
                            + " cannot be applied when booking status is "
                            + currentStatus
            );
        }

        int updated = bookingRepository.transition(
                merchant.getId(),
                booking.bookingId(),
                allowedStatuses,
                targetStatus
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Booking status changed during webhook processing"
            );
        }

        // Chỉ booking có tracking link mới được tính là conversion.
        if (booking.trackingLinkId() != null) {
            if (request.event() == Event.PAYMENT_CONFIRMED) {
                financeRepository.createCommissionForApprovedBooking(
                        merchant.getId(),
                        booking.bookingId()
                );
            } else if (request.event() == Event.PAYMENT_REFUNDED) {
                financeRepository.reverseCommissionForBooking(
                        merchant.getId(),
                        booking.bookingId()
                );
            }
        }

        return new Result(
                booking.bookingId(),
                request.event(),
                currentStatus,
                false,
                Instant.now()
        );
    }

    private MerchantBookingWebhookDtos.Request parseRequest(String rawBody) {
        try {
            MerchantBookingWebhookDtos.Request request =
                    objectMapper.readValue(
                            rawBody,
                            MerchantBookingWebhookDtos.Request.class
                    );

            if (request.bookingId() == null || request.event() == null) {
                throw new IllegalArgumentException(
                        "bookingId and event are required"
                );
            }

            return request;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Webhook body is not valid JSON"
            );
        }
    }

    private String normalizeMerchantCode(String value) {
        if (value == null || value.isBlank()) {
            throw invalidCredentials();
        }

        String merchantCode = value.trim();

        if (!merchantCode.matches("[A-Za-z0-9_-]{2,100}")) {
            throw invalidCredentials();
        }

        return merchantCode;
    }

    private long parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "X-Webhook-Timestamp header is required"
            );
        }

        final long timestamp;
        try {
            timestamp = Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "X-Webhook-Timestamp must be Unix time in seconds"
            );
        }

        Instant signedAt = Instant.ofEpochSecond(timestamp);
        Duration difference = Duration.between(signedAt, Instant.now()).abs();

        if (difference.compareTo(MAX_CLOCK_SKEW) > 0) {
            throw new BadCredentialsException(
                    "Webhook timestamp has expired"
            );
        }

        return timestamp;
    }

    private void verifySignature(
            String secret,
            long timestamp,
            String signatureHeader,
            String rawBody
    ) {
        if (signatureHeader == null
                || !signatureHeader.startsWith("sha256=")) {
            throw invalidCredentials();
        }

        String providedHex = signatureHeader.substring("sha256=".length())
                .trim()
                .toLowerCase(Locale.ROOT);

        String signedContent = timestamp + "." + rawBody;
        String expectedHex = calculateHmac(secret, signedContent);

        boolean matches = MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.US_ASCII),
                providedHex.getBytes(StandardCharsets.US_ASCII)
        );

        if (!matches) {
            throw invalidCredentials();
        }
    }

    private String calculateHmac(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(key);

            return HexFormat.of().formatHex(
                    mac.doFinal(content.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not verify webhook signature",
                    exception
            );
        }
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException(
                "Invalid merchant webhook credentials"
        );
    }

    @Transactional(readOnly = true)
    public MerchantBookingWebhookDtos.Credential getCredential(
            CustomUserDetails user
    ) {
        if (user == null) {
            throw new AccessDeniedException("Merchant account is required");
        }

        Merchant merchant = merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "This user is not linked to a merchant"
                ));

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new AccessDeniedException("Merchant account is not active");
        }

        return new MerchantBookingWebhookDtos.Credential(
                merchant.getCode(),
                deriveSigningSecret(merchant.getId()),
                "HMAC-SHA256",
                "UnixTimestamp + '.' + exact raw JSON body",
                "sha256=<lowercase hex digest>"
        );
    }

    private String deriveSigningSecret(UUID merchantId) {
        String masterSecret = environment.getProperty(
                "merchant.webhook.master-secret"
        );

        if (masterSecret == null
                || masterSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "MERCHANT_WEBHOOK_MASTER_SECRET must contain at least 32 bytes"
            );
        }

        return calculateHmac(
                masterSecret,
                "merchant-webhook:v1:" + merchantId
        );
    }
}