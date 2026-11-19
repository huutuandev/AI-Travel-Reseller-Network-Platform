package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantReportDtos;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.MerchantReportQueryRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantReportService {
    private static final Set<String> CAMPAIGN_STATUSES = Set.of(
            "DRAFT",
            "PENDING_REVIEW",
            "ACTIVE",
            "PAUSED",
            "ENDED",
            "CANCELLED"
    );

    private final MerchantReportQueryRepository reportRepository;
    private final MerchantRepository merchantRepository;

    @Transactional(readOnly = true)
    public MerchantReportDtos.CampaignPageResponse campaignReport(
            int page,
            int size,
            String search,
            String status,
            UUID productId,
            UUID campaignId,
            Instant from,
            Instant to,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);
        validateDateRange(from, to);

        String normalizedSearch = normalizeSearch(search);
        String normalizedStatus = normalizeStatus(status);

        if (!normalizedStatus.isEmpty()
                && !CAMPAIGN_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid campaign status");
        }

        long total = reportRepository.countCampaignReports(
                merchant.getId(),
                normalizedSearch,
                normalizedStatus,
                productId,
                campaignId
        );

        var content = reportRepository.findCampaignReportPage(
                merchant.getId(),
                normalizedSearch,
                normalizedStatus,
                productId,
                campaignId,
                from,
                to,
                page,
                size
        );

        return new MerchantReportDtos.CampaignPageResponse(
                content,
                page,
                size,
                total,
                totalPages(total, size)
        );
    }

    @Transactional(readOnly = true)
    public MerchantReportDtos.ResellerPageResponse resellerReport(
            int page,
            int size,
            String search,
            UUID campaignId,
            Instant from,
            Instant to,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);
        validateDateRange(from, to);

        String normalizedSearch = normalizeSearch(search);

        long total = reportRepository.countResellerReports(
                merchant.getId(),
                normalizedSearch,
                campaignId
        );

        var content = reportRepository.findResellerReportPage(
                merchant.getId(),
                normalizedSearch,
                campaignId,
                from,
                to,
                page,
                size
        );

        return new MerchantReportDtos.ResellerPageResponse(
                content,
                page,
                size,
                total,
                totalPages(total, size)
        );
    }

    private Merchant requireActiveMerchant(CustomUserDetails user) {
        if (user == null) {
            throw new AccessDeniedException("Merchant account is required");
        }

        Merchant merchant = merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "This user is not linked to a merchant"
                ));

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new AccessDeniedException(
                    "Merchant account is not active"
            );
        }

        return merchant;
    }

    private String normalizeSearch(String search) {
        String normalized = search == null ? "" : search.trim();
        if (normalized.length() > 200) {
            throw new IllegalArgumentException(
                    "search must not exceed 200 characters"
            );
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        return status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);
    }

    private void validateDateRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "from must be earlier than to"
            );
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be zero or greater"
            );
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }
    }

    private int totalPages(long total, int size) {
        return total == 0
                ? 0
                : (int) ((total + size - 1) / size);
    }
}