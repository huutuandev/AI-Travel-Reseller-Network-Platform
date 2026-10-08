package com.aitravel.reseller.dto.respone;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    @Schema(description = "Product ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private UUID id;

    @Schema(description = "Merchant ID owning this product", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private UUID merchantId;

    @Schema(description = "Product name", example = "Vé Bà Nà Hills Cáp Treo")
    private String name;

    @Schema(description = "Product category", example = "VE_THAM_QUAN")
    private String category;

    @Schema(description = "Product description", example = "Vé tham quan Bà Nà Hills bao gồm cáp treo khứ hồi")
    private String description;

    @Schema(description = "Original price before discount", example = "950000.00")
    private BigDecimal originalPrice;

    @Schema(description = "Sale price", example = "850000.00")
    private BigDecimal salePrice;

    @Schema(description = "Booking landing URL", example = "https://banahills.sunworld.vn/booking")
    private String bookingUrl;

    @Schema(description = "Structured FAQ in JSON format")
    private String faqStructured;

    @Schema(description = "List of image or video URLs")
    private List<String> mediaAssets;

    @Schema(description = "Product status", example = "ACTIVE")
    private String status;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
