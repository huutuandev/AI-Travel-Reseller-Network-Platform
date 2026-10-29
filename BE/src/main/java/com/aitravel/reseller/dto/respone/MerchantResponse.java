package com.aitravel.reseller.dto.respone;

import java.math.BigDecimal;

public record MerchantResponse(
        long successfulOrders,
        BigDecimal revenue
) {}