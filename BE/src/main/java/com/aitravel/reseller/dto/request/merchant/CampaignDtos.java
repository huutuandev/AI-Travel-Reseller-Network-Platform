package com.aitravel.reseller.dto.request.merchant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class CampaignDtos {
    private CampaignDtos() {}

    @Schema(name = "MerchantCampaignUpsertRequest")
    public record UpsertRequest(
            @NotNull
            UUID productId,

            @NotBlank
            @Size(max = 255)
            String title,

            @Size(max = 10000)
            String description,

            @NotBlank
            @Pattern(regexp = "PERCENTAGE|FIXED_AMOUNT")
            String commissionType,

            @NotNull
            @DecimalMin("0.00")
            @Digits(integer = 8, fraction = 2)
            BigDecimal commissionRate,


                // AUD: Đô la Úc
                // CAD: Đô la Canada
                // CHF: Franc Thụy Sĩ
                // CNY: Nhân dân tệ Trung Quốc
                // EUR: Euro
                // GBP: Bảng Anh
                // HKD: Đô la Hồng Kông
                // IDR: Rupiah Indonesia
                // INR: Rupee Ấn Độ
                // JPY: Yên Nhật
                // KRW: Won Hàn Quốc
                // MYR: Ringgit Malaysia
                // NZD: Đô la New Zealand
                // PHP: Peso Philippines
                // SGD: Đô la Singapore
                // THB: Baht Thái Lan
                // TWD: Đô la Đài Loan mới
                // USD: Đô la Mỹ
                // VND: Đồng Việt Nam
            @Pattern(
                regexp = "AUD|CAD|CHF|CNY|EUR|GBP|HKD|IDR|INR|JPY|KRW|MYR|NZD|PHP|SGD|THB|TWD|USD|VND",
                message = "commissionCurrency must be a supported currency"
            )
            String commissionCurrency,

            @NotNull
            @Positive
            @Max(3650)
            Integer cookieWindowDays,

            @Size(max = 20000)
            String termsText,

            Instant startDate,
            Instant endDate
    ) {}

    public record StatusRequest(
            @NotBlank
            @Pattern(regexp = "ACTIVE|PAUSED|ENDED|CANCELLED")
            String status
    ) {}

    public record ProductSummary(
            UUID id,
            String title,
            BigDecimal price,
            String currency,
            String availabilityStatus
    ) {}

    public record CampaignResponse(
            UUID id,
            ProductSummary product,
            String title,
            String description,
            String commissionType,
            BigDecimal commissionRate,
            String commissionCurrency,
            Integer cookieWindowDays,
            String termsText,
            Integer termsVersion,
            Instant startDate,
            Instant endDate,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CampaignPageResponse(
            List<CampaignResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}