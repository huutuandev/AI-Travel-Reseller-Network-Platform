package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.respone.merchant.ResellerCommissionRankingResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.ResellerCommissionRankingRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.ResellerCommissionRankingService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResellerCommissionRankingServiceTest {
    private final ResellerCommissionRankingRepository repository = mock(ResellerCommissionRankingRepository.class);
    private final MerchantRepository merchants = mock(MerchantRepository.class);
    private final ResellerCommissionRankingService service = new ResellerCommissionRankingService(repository, merchants);

    @Test
    void returnsTheRequestedPageWithoutResettingGlobalRanks() {
        UUID merchantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var user = new CustomUserDetails(userId, "merchant@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        try {
            when(merchants.findByUserId(userId)).thenReturn(Optional.of(
                    Merchant.builder().id(merchantId).status("ACTIVE").build()));
            var first = entry(1, "Sam", "42.00", 1);
            var second = entry(2, "Mia", "0.00", 0);
            var third = entry(3, "Coastline", "0.00", 0);
            when(repository.findRankings(merchantId, "USD")).thenReturn(List.of(first, second, third));

            var result = service.ranking("usd", 1, 2);

            assertEquals("USD", result.currency());
            assertEquals(3, result.totalElements());
            assertEquals(2, result.totalPages());
            assertEquals(List.of(third), result.rankings());
            assertEquals(3, result.rankings().getFirst().rank());
            verify(repository).findRankings(merchantId, "USD");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void refusesMerchantWithoutAnActiveProfile() {
        UUID userId = UUID.randomUUID();
        var user = new CustomUserDetails(userId, "merchant@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        try {
            when(merchants.findByUserId(userId)).thenReturn(Optional.of(
                    Merchant.builder().id(UUID.randomUUID()).status("SUSPENDED").build()));
            assertThrows(AccessDeniedException.class, () -> service.ranking("USD", 0, 3));
            verifyNoInteractions(repository);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void rejectsInvalidPaginationAndCurrencyBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> service.ranking("USD", -1, 3));
        assertThrows(IllegalArgumentException.class, () -> service.ranking("USD", 0, 101));
        assertThrows(IllegalArgumentException.class, () -> service.ranking("US$", 0, 3));
        verifyNoInteractions(repository);
    }

    private ResellerCommissionRankingResponse.Entry entry(long rank, String name, String amount, long bookings) {
        return new ResellerCommissionRankingResponse.Entry(rank, UUID.randomUUID(), name,
                "test@example.com", new BigDecimal(amount), bookings);
    }
}
