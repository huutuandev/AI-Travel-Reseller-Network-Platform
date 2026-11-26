package com.aitravel.reseller.dto.respone.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MerchantFinanceDtos {
    private MerchantFinanceDtos() {}

    public record CommissionPage(
            List<CommissionItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record CommissionItem(
            UUID commissionId,
            UUID bookingId,
            String bookingStatus,
            UUID resellerId,
            String resellerName,
            UUID campaignId,
            String campaignTitle,
            UUID productId,
            String productTitle,
            BigDecimal bookingAmount,
            String bookingCurrency,
            BigDecimal commissionAmount,
            String currency,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CommissionStatusRequest(
            @NotBlank String status
    ) {}

    public record PayoutPage(
            List<PayoutSummary> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record PayoutSummary(
            UUID payoutId,
            UUID resellerId,
            String resellerName,
            BigDecimal totalAmount,
            String currency,
            String status,
            String paymentReference,
            long itemCount,
            Instant createdAt,
            Instant processedAt
    ) {}

    public record PayoutLine(
            UUID payoutItemId,
            UUID commissionId,
            UUID bookingId,
            String campaignTitle,
            BigDecimal amount,
            String currency
    ) {}

    public record PayoutDetail(
            PayoutSummary payout,
            List<PayoutLine> items
    ) {}

    public record CreatePayoutRequest(
            @NotNull UUID resellerId,
            @NotBlank
            @Pattern(regexp = "(?i)^[A-Z]{3}$")
            String currency,
            @NotEmpty
            @Size(max = 500)
            List<@NotNull UUID> commissionIds
    ) {}

    public record PayoutStatusRequest(
            @NotBlank String status,
            @Size(max = 255) String paymentReference
    ) {}
}