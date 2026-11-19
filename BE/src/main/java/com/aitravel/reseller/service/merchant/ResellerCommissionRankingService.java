package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.ResellerCommissionRankingResponse;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.ResellerCommissionRankingRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResellerCommissionRankingService {
    private final ResellerCommissionRankingRepository repository;
    private final MerchantRepository merchants;

    @Transactional(readOnly = true)
    public ResellerCommissionRankingResponse ranking(String currency, int page, int size) {
        if (currency == null || !currency.matches("(?i)[a-z]{3}")) {
            throw new IllegalArgumentException("currency must be a three-letter ISO code");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page must be nonnegative and size must be between 1 and 100");
        }
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        var all = repository.findRankings(currentMerchantId(), normalizedCurrency);
        long start = (long) page * size;
        int from = (int) Math.min(start, all.size());
        int to = (int) Math.min(start + size, all.size());
        int totalPages = (int) ((all.size() + (long) size - 1) / size);
        return new ResellerCommissionRankingResponse(normalizedCurrency, page, size, totalPages,
                all.size(), all.subList(from, to));
    }

    private UUID currentMerchantId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        if (!user.isEnabled() || auth.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_MERCHANT".equals(authority.getAuthority()))) {
            throw new AccessDeniedException("Merchant role required");
        }
        var merchant = merchants.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Merchant profile required"));
        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new AccessDeniedException("Active merchant profile required");
        }
        return merchant.getId();
    }
}
