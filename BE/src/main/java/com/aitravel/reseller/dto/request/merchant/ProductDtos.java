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

            @NotBlank
            @Pattern(regexp = "[A-Z]{3}")
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