package com.aitravel.reseller.dto.respone.merchant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResellerCommissionRankingResponse(
        String currency, int page, int size, int totalPages, long totalElements, List<Entry> rankings) {

    public record Entry(long rank, UUID resellerId, String resellerName, String email,
                        BigDecimal commission, long bookings) {}
}
