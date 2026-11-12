package com.aitravel.reseller.service.reseller;

import com.aitravel.reseller.dto.respone.reseller.ResellerDashboardResponse;
import com.aitravel.reseller.repository.ResellerProfileRepository;
import com.aitravel.reseller.repository.reseller.ResellerDashboardRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResellerDashboardService {

    private final ResellerDashboardRepository dashboard;
    private final ResellerProfileRepository profiles;

    public ResellerDashboardResponse getDashboard() {
        UUID resellerId = currentResellerId();
        var pending = dashboard.commissionAmounts(resellerId, List.of("PENDING"));
        var approved = dashboard.commissionAmounts(resellerId, List.of("APPROVED", "PAYABLE"));
        var paid = dashboard.commissionAmounts(resellerId, List.of("PAID", "PARTIALLY_PAID"));
        var earned = dashboard.commissionAmounts(resellerId,
                List.of("PENDING", "APPROVED", "PAYABLE", "PAID", "PARTIALLY_PAID"));
        long totalClicks = dashboard.totalClicks(resellerId);
        long totalConversions = dashboard.totalConversions(resellerId);
        BigDecimal conversionRate = totalClicks == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalConversions)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalClicks), 2, RoundingMode.HALF_UP);

        return new ResellerDashboardResponse(
                new ResellerDashboardResponse.Overview(
                        totalClicks, totalConversions, conversionRate, pending, earned),
                concat(summary("PENDING", pending), summary("APPROVED", approved), summary("PAID", paid)),
                dashboard.clickTrend(resellerId),
                dashboard.campaigns(resellerId),
                dashboard.trackingLinks(resellerId),
                dashboard.recentCommissions(resellerId));
    }

    @SafeVarargs
    private final <T> List<T> concat(List<T>... lists) {
        return java.util.Arrays.stream(lists).flatMap(List::stream).toList();
    }

    private List<ResellerDashboardResponse.CommissionSummary> summary(
            String status, List<ResellerDashboardResponse.MoneyAmount> amounts) {
        return amounts.stream()
                .map(amount -> new ResellerDashboardResponse.CommissionSummary(
                        status, amount.currency(), amount.amount()))
                .toList();
    }

    private UUID currentResellerId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        boolean resellerRole = auth.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RESELLER".equals(authority.getAuthority()));
        if (!user.isEnabled() || !resellerRole) {
            throw new AccessDeniedException("Reseller role required");
        }
        var profile = profiles.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Reseller profile required"));
        if (!"ACTIVE".equals(profile.getStatus())) {
            throw new AccessDeniedException("Active reseller profile required");
        }
        return profile.getId();
    }
}
