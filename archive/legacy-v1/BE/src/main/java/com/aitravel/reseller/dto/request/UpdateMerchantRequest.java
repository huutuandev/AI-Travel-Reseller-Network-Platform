package com.aitravel.reseller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMerchantRequest {

    @Schema(description = "Merchant business name", example = "Sun World Ba Na Hills")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Schema(description = "Merchant logo URL", example = "https://cdn.example.com/logo.png")
    private String logoUrl;

    @Schema(description = "Merchant contact phone", example = "0901234567")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Schema(description = "Merchant address", example = "An Son, Hoa Ninh, Hoa Vang, Da Nang")
    private String address;

    @Schema(description = "Merchant official website", example = "https://banahills.sunworld.vn")
    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;
}
