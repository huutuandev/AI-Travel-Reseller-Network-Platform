package com.aitravel.reseller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @Schema(description = "Product name", example = "Vé Bà Nà Hills Cáp Treo")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Schema(description = "Product category", example = "VE_THAM_QUAN", allowableValues = {"VE_THAM_QUAN", "BUFFET", "TOUR_NGAY", "SPA", "XE_DUA_DON"})
    @Pattern(regexp = "^(VE_THAM_QUAN|BUFFET|TOUR_NGAY|SPA|XE_DUA_DON)$", message = "Category must be one of: VE_THAM_QUAN, BUFFET, TOUR_NGAY, SPA, XE_DUA_DON")
    private String category;

    @Schema(description = "Product description", example = "Vé tham quan Bà Nà Hills bao gồm cáp treo khứ hồi")
    private String description;

    @Schema(description = "Original price before discount", example = "950000.00")
    @DecimalMin(value = "0.0", inclusive = true, message = "Original price must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "Original price format is invalid (up to 10 integer digits and 2 decimals)")
    private BigDecimal originalPrice;

    @Schema(description = "Sale price", example = "850000.00")
    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sale price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Sale price format is invalid (up to 10 integer digits and 2 decimals)")
    private BigDecimal salePrice;

    @Schema(description = "Booking landing URL", example = "https://banahills.sunworld.vn/booking")
    @Pattern(regexp = "^https?://.*$", message = "Booking URL must be a valid HTTP or HTTPS URL")
    private String bookingUrl;

    @Schema(description = "Structured FAQ in JSON format", example = "[{\"question\":\"Thời gian mở cửa?\",\"answer\":\"08:00 - 17:00\"}]")
    private String faqStructured;

    @Schema(description = "List of image or video URLs", example = "[\"https://cdn.example.com/bana1.jpg\", \"https://cdn.example.com/bana2.jpg\"]")
    private List<String> mediaAssets;

    @Schema(description = "Product status", example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "ARCHIVED"})
    @Pattern(regexp = "^(DRAFT|ACTIVE|ARCHIVED)$", message = "Status must be one of: DRAFT, ACTIVE, ARCHIVED")
    private String status;
}
