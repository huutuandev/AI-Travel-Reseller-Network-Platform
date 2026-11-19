package com.aitravel.reseller.dto.request.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class VoucherDtos {
    private VoucherDtos() {}

    @Schema(name = "MerchantVoucherUpsertRequest")
    public record UpsertRequest(
            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9_-]{2,100}")
            String code,

            @NotBlank
            @Size(max = 255)
            String name,

            @Size(max = 10000)
            String description,

            @NotBlank
            @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT")
            String discountType,

            @NotNull
            @DecimalMin("0.00")
            @Digits(integer = 10, fraction = 2)
            BigDecimal discountValue,

            @DecimalMin("0.00")
            @Digits(integer = 10, fraction = 2)
            BigDecimal maxDiscountAmount,

            @NotNull
            @DecimalMin("0.00")
            @Digits(integer = 10, fraction = 2)
            BigDecimal minOrderAmount,

            @PositiveOrZero
            Long usageLimit,

            @Positive
            Integer perCustomerLimit,

            Instant startsAt,
            Instant expiresAt,

            @Schema(
                    description = "Để null nếu Voucher dùng chung ở cấp Merchant",
                    nullable = true
            )
            UUID campaignApplicationId
    ) {}

    @Schema(name = "MerchantVoucherStatusRequest")
    public record StatusRequest(
            @NotBlank
            @Pattern(regexp = "ACTIVE|DISABLED")
            String status
    ) {}

    @Schema(name = "MerchantVoucherResponse")
    public record VoucherResponse(
            UUID id,
            UUID campaignApplicationId,
            String code,
            String name,
            String description,
            String discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            Long usageLimit,
            Long usageCount,
            Integer perCustomerLimit,
            Instant startsAt,
            Instant expiresAt,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @Schema(name = "MerchantVoucherPageResponse")
    public record PageResponse(
            List<VoucherResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}