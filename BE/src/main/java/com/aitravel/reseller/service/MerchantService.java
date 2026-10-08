package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.respone.MerchantResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private static final String CONFIRMED = "CONFIRMED";

    private final MerchantRepository merchantRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public MerchantResponse getDashboard(UUID userId) {
        Merchant merchant = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant profile not found for this account"));

        long orders = orderRepository.countByMerchantIdAndStatus(merchant.getId(), CONFIRMED);
        BigDecimal revenue = orderRepository.sumAmountByMerchantIdAndStatus(merchant.getId(), CONFIRMED);

        return new MerchantResponse(orders, revenue != null ? revenue : BigDecimal.ZERO);
    }
}