package com.aitravel.reseller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRequest {
    @NotNull(message = "Merchant ID không được để trống")
    private UUID merchantId;

    @NotNull(message = "Product ID không được để trống")
    private UUID productId;

    @NotBlank(message = "Tên chiến dịch không được để trống")
    private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Tỷ lệ hoa hồng không được để trống")
    private BigDecimal commissionRate;

    private BigDecimal resellerSplitRate;

    private String guidelines;

    private String status;
}
