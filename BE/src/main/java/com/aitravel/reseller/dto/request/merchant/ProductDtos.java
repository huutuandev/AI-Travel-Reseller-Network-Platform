package com.aitravel.reseller.dto.request.merchant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public final class ProductDtos {
    private ProductDtos() {}

    @Schema(name = "MerchantProductUpsertRequest")
    public record UpsertRequest(
            @NotBlank
            @Size(max = 255)
            String title,

            @Size(max = 10000)
            String description,

            @Size(max = 50)
            String categoryCode,

            @Size(max = 100)
            String destinationCode,

            @NotNull
            List<Object> images,

            @NotNull
            @DecimalMin("0.00")
            BigDecimal price,

            @DecimalMin("0.00")
            BigDecimal salePrice,
        

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
            @NotBlank
            @Pattern(
                regexp = "AUD|CAD|CHF|CNY|EUR|GBP|HKD|IDR|INR|JPY|KRW|MYR|NZD|PHP|SGD|THB|TWD|USD|VND",
                message = "currency must be a supported currency"
            )
            String currency,

            @NotBlank
            @Pattern(regexp = "AVAILABLE|UNAVAILABLE|SOLD_OUT|DRAFT")
            String availabilityStatus,

            @NotBlank
            @Size(max = 2048)
            String officialCheckoutUrl
    ) {}

    public record AvailabilityRequest(
            @NotBlank
            @Pattern(regexp = "AVAILABLE|UNAVAILABLE|SOLD_OUT|DRAFT")
            String availabilityStatus
    ) {}

    public record ProductResponse(
            UUID id,
            String title,
            String slug,
            String description,
            String categoryCode,
            String destinationCode,
            List<Object> images,
            BigDecimal price,
            BigDecimal salePrice,
            String currency,
            String availabilityStatus,
            String officialCheckoutUrl,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProductPageResponse(
            List<ProductResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}