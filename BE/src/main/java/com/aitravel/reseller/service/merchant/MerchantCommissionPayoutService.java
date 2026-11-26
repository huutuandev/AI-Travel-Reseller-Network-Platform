package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantFinanceDtos;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.MerchantCommissionPayoutRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantCommissionPayoutService {
    private static final Set<String> COMMISSION_STATUSES = Set.of(
            "PENDING",
            "APPROVED",
            "PAYABLE",
            "PAID",
            "PARTIALLY_PAID",
            "CANCELLED",
            "REVERSED"
    );

    private static final Set<String> PAYOUT_STATUSES = Set.of(
            "PROCESSING",
            "COMPLETED",
            "FAILED"
    );

    private final MerchantCommissionPayoutRepository repository;
    private final MerchantRepository merchantRepository;

    @Transactional(readOnly = true)
    public MerchantFinanceDtos.CommissionPage listCommissions(
            int page,
            int size,
            String status,
            UUID resellerId,
            UUID campaignId,
            UUID bookingId,
            Instant from,
            Instant to,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);
        validateRange(from, to);

        String normalizedStatus = normalize(status);
        if (!normalizedStatus.isEmpty()
                && !COMMISSION_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid commission status");
        }

        long total = repository.countCommissions(
                merchant.getId(),
                normalizedStatus,
                resellerId,
                campaignId,
                bookingId,
                from,
                to
        );

        var items = repository.findCommissionPage(
                merchant.getId(),
                normalizedStatus,
                resellerId,
                campaignId,
                bookingId,
                from,
                to,
                page,
                size
        );

        return new MerchantFinanceDtos.CommissionPage(
                items,
                page,
                size,
                total,
                totalPages(total, size)
        );
    }

    @Transactional(readOnly = true)
    public MerchantFinanceDtos.CommissionItem getCommission(
            UUID commissionId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        return repository.findCommission(merchant.getId(), commissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commission not found"
                ));
    }

    @Transactional
    public MerchantFinanceDtos.CommissionItem changeCommissionStatus(
            UUID commissionId,
            MerchantFinanceDtos.CommissionStatusRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        if (request == null || request.status() == null
                || request.status().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        String targetStatus = normalize(request.status());
        if (!Set.of("APPROVED", "PAYABLE", "CANCELLED", "REVERSED")
                .contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "status must be APPROVED, PAYABLE, CANCELLED, or REVERSED"
            );
        }

        MerchantCommissionPayoutRepository.CommissionState current =
                repository.findCommissionStateForUpdate(
                                merchant.getId(),
                                commissionId
                        )
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Commission not found"
                        ));

        if (current.hasPayoutItem()) {
            throw new IllegalStateException(
                    "Commission already belongs to a payout and cannot be changed"
            );
        }

        if (!isAllowedCommissionTransition(
                current.status(),
                current.bookingStatus(),
                targetStatus
        )) {
            throw new IllegalStateException(
                    "Commission cannot change from "
                            + current.status() + " to " + targetStatus
            );
        }

        int updated = repository.updateCommissionStatus(
                merchant.getId(),
                commissionId,
                current.status(),
                targetStatus
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Commission status changed concurrently; reload and try again"
            );
        }

        return repository.findCommission(merchant.getId(), commissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Commission not found"
                ));
    }

    @Transactional(readOnly = true)
    public MerchantFinanceDtos.PayoutPage listPayouts(
            int page,
            int size,
            String status,
            UUID resellerId,
            Instant from,
            Instant to,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);
        validateRange(from, to);

        String normalizedStatus = normalize(status);
        if (!normalizedStatus.isEmpty()
                && !PAYOUT_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid payout status");
        }

        long total = repository.countPayouts(
                merchant.getId(),
                normalizedStatus,
                resellerId,
                from,
                to
        );

        var items = repository.findPayoutPage(
                merchant.getId(),
                normalizedStatus,
                resellerId,
                from,
                to,
                page,
                size
        );

        return new MerchantFinanceDtos.PayoutPage(
                items,
                page,
                size,
                total,
                totalPages(total, size)
        );
    }

    @Transactional(readOnly = true)
    public MerchantFinanceDtos.PayoutDetail getPayout(
            UUID payoutId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        return findPayoutDetail(merchant.getId(), payoutId);
    }

    @Transactional
    public MerchantFinanceDtos.PayoutDetail createPayout(
            MerchantFinanceDtos.CreatePayoutRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (request.commissionIds() == null
                || request.commissionIds().isEmpty()
                || request.commissionIds().size() > 500) {
            throw new IllegalArgumentException(
                    "commissionIds must contain between 1 and 500 IDs"
            );
        }

        if (new HashSet<>(request.commissionIds()).size()
                != request.commissionIds().size()) {
            throw new IllegalArgumentException(
                    "commissionIds must not contain duplicates"
            );
        }

        String currency = request.currency()
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "currency must be a three-letter code"
            );
        }

        List<UUID> eligibleIds = repository.lockEligibleCommissionIds(
                merchant.getId(),
                request.resellerId(),
                currency,
                request.commissionIds()
        );

        if (eligibleIds.size() != request.commissionIds().size()) {
            throw new IllegalStateException(
                    "Every commission must belong to this merchant and reseller, use the same currency, be PAYABLE, and not already be in a payout"
            );
        }

        UUID payoutId = repository.createPayout(
                merchant.getId(),
                request.resellerId(),
                currency
        );

        for (UUID commissionId : eligibleIds) {
            repository.addCommissionToPayout(payoutId, commissionId);
        }

        return findPayoutDetail(merchant.getId(), payoutId);
    }

    @Transactional
    public MerchantFinanceDtos.PayoutDetail changePayoutStatus(
            UUID payoutId,
            MerchantFinanceDtos.PayoutStatusRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        if (request == null || request.status() == null
                || request.status().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        String targetStatus = normalize(request.status());
        if (!PAYOUT_STATUSES.contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "status must be PROCESSING, COMPLETED, or FAILED"
            );
        }

        String currentStatus = repository.findPayoutStatusForUpdate(
                        merchant.getId(),
                        payoutId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payout not found"
                ));

        boolean validTransition =
                ("PROCESSING".equals(currentStatus)
                        && Set.of("COMPLETED", "FAILED").contains(targetStatus))
                || ("FAILED".equals(currentStatus)
                        && "PROCESSING".equals(targetStatus));

        if (!validTransition) {
            throw new IllegalStateException(
                    "Payout cannot change from "
                            + currentStatus + " to " + targetStatus
            );
        }

        String paymentReference = normalizeOptional(
                request.paymentReference()
        );

        if ("COMPLETED".equals(targetStatus)
                && (paymentReference == null
                    || paymentReference.length() > 255)) {
            throw new IllegalArgumentException(
                    "paymentReference is required to mark a payout COMPLETED"
            );
        }

        if ("COMPLETED".equals(targetStatus)) {
            long itemCount = repository.countPayoutItems(
                    merchant.getId(),
                    payoutId
            );

            if (itemCount == 0) {
                throw new IllegalStateException(
                        "An empty payout cannot be completed"
                );
            }

            int paidCount = repository.markPayoutCommissionsPaid(
                    merchant.getId(),
                    payoutId
            );

            if (paidCount != itemCount) {
                throw new IllegalStateException(
                        "Not all payout commissions are PAYABLE; payout was not completed"
                );
            }
        }

        int updated = repository.updatePayoutStatus(
                merchant.getId(),
                payoutId,
                currentStatus,
                targetStatus,
                paymentReference
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Payout status changed concurrently; reload and try again"
            );
        }

        return findPayoutDetail(merchant.getId(), payoutId);
    }

    @Transactional(readOnly = true)
    public byte[] exportPayoutCsv(
            UUID payoutId,
            CustomUserDetails user
    ) {
        MerchantFinanceDtos.PayoutDetail detail =
                getPayout(payoutId, user);

        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Payout ID,Reseller,Status,Payment Reference,")
                .append("Commission ID,Booking ID,Campaign,Amount,Currency\r\n");

        for (MerchantFinanceDtos.PayoutLine line : detail.items()) {
            csv.append(csvCell(detail.payout().payoutId().toString())).append(',')
                    .append(csvCell(detail.payout().resellerName())).append(',')
                    .append(csvCell(detail.payout().status())).append(',')
                    .append(csvCell(detail.payout().paymentReference())).append(',')
                    .append(csvCell(line.commissionId().toString())).append(',')
                    .append(csvCell(line.bookingId().toString())).append(',')
                    .append(csvCell(line.campaignTitle())).append(',')
                    .append(csvCell(line.amount().toPlainString())).append(',')
                    .append(csvCell(line.currency())).append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private MerchantFinanceDtos.PayoutDetail findPayoutDetail(
            UUID merchantId,
            UUID payoutId
    ) {
        MerchantFinanceDtos.PayoutSummary summary =
                repository.findPayoutSummary(merchantId, payoutId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Payout not found"
                        ));

        return new MerchantFinanceDtos.PayoutDetail(
                summary,
                repository.findPayoutLines(merchantId, payoutId)
        );
    }

    private boolean isAllowedCommissionTransition(
            String currentStatus,
            String bookingStatus,
            String targetStatus
    ) {
        if ("PENDING".equals(currentStatus)
                && "APPROVED".equals(targetStatus)) {
            return true;
        }

        if ("APPROVED".equals(currentStatus)
                && "PAYABLE".equals(targetStatus)) {
            return "COMPLETED".equals(bookingStatus);
        }

        if (Set.of("PENDING", "APPROVED", "PAYABLE")
                .contains(currentStatus)
                && "CANCELLED".equals(targetStatus)) {
            return "CANCELLED".equals(bookingStatus);
        }

        if (Set.of("PENDING", "APPROVED", "PAYABLE")
                .contains(currentStatus)
                && "REVERSED".equals(targetStatus)) {
            return "REFUNDED".equals(bookingStatus);
        }

        return false;
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

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "from must be earlier than to"
            );
        }
    }

    private int totalPages(long total, int size) {
        return total == 0 ? 0 : (int) ((total + size - 1) / size);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty()
                && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}