package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantResellerDtos;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.MerchantResellerRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantResellerService {
    private static final Set<String> APPLICATION_STATUSES = Set.of(
            "PENDING", "APPROVED", "REJECTED", "WITHDRAWN", "SUSPENDED"
    );

    private static final Set<String> TARGET_STATUSES = Set.of(
            "APPROVED", "REJECTED", "SUSPENDED"
    );

    private final MerchantResellerRepository resellerRepository;
    private final MerchantRepository merchantRepository;

    @Transactional(readOnly = true)
    public MerchantResellerDtos.PageResponse list(
            int page,
            int size,
            String search,
            String status,
            UUID campaignId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);

        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.length() > 200) {
            throw new IllegalArgumentException(
                    "search must not exceed 200 characters"
            );
        }

        String normalizedStatus = normalizeStatus(status);
        if (!normalizedStatus.isEmpty()
                && !APPLICATION_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid campaign application status"
            );
        }

        long total = resellerRepository.countResellers(
                merchant.getId(),
                normalizedSearch,
                campaignId,
                normalizedStatus
        );

        List<MerchantResellerDtos.ResellerSummary> content =
                resellerRepository.findResellerPage(
                        merchant.getId(),
                        normalizedSearch,
                        campaignId,
                        normalizedStatus,
                        page,
                        size
                );

        int totalPages = total == 0
                ? 0
                : (int) Math.ceil((double) total / size);

        return new MerchantResellerDtos.PageResponse(
                content,
                page,
                size,
                total,
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public MerchantResellerDtos.ResellerDetail get(
            UUID resellerId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        var reseller = resellerRepository.findReseller(
                        merchant.getId(),
                        resellerId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reseller not found"
                ));

        List<MerchantResellerDtos.CampaignParticipation> applications =
                resellerRepository.findApplications(
                        merchant.getId(),
                        List.of(resellerId),
                        null,
                        null
                );

        return new MerchantResellerDtos.ResellerDetail(
                reseller.resellerId(),
                reseller.fullName(),
                reseller.email(),
                reseller.phone(),
                reseller.resellerProfileStatus(),
                reseller.resellerSince(),
                applications
        );
    }

    @Transactional
    public MerchantResellerDtos.CampaignParticipation changeApplicationStatus(
            UUID applicationId,
            MerchantResellerDtos.StatusRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        if (request == null || request.status() == null
                || request.status().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        String targetStatus = request.status()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!TARGET_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "status must be APPROVED, REJECTED, or SUSPENDED"
            );
        }

        String currentStatus = resellerRepository
                .findStatusForUpdate(merchant.getId(), applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign application not found"
                ));

        if (!currentStatus.equals(targetStatus)
                && !isAllowedTransition(currentStatus, targetStatus)) {
            throw new IllegalStateException(
                    "Campaign application cannot change from "
                            + currentStatus + " to " + targetStatus
            );
        }

        if (!currentStatus.equals(targetStatus)) {
            int updated = resellerRepository.updateApplicationStatus(
                    applicationId,
                    targetStatus
            );

            if (updated != 1) {
                throw new IllegalStateException(
                        "Campaign application status was not updated"
                );
            }
        }

        return resellerRepository.findApplicationById(
                        merchant.getId(),
                        applicationId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign application not found"
                ));
    }

    private boolean isAllowedTransition(
            String currentStatus,
            String targetStatus
    ) {
        return switch (currentStatus) {
            case "PENDING" ->
                    "APPROVED".equals(targetStatus)
                            || "REJECTED".equals(targetStatus);
            case "APPROVED" -> "SUSPENDED".equals(targetStatus);
            case "SUSPENDED" -> "APPROVED".equals(targetStatus);
            default -> false;
        };
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

    private String normalizeStatus(String status) {
        return status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);
    }
}