package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.VoucherDtos;
import com.aitravel.reseller.entity.CampaignApplication;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Voucher;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.CampaignApplicationRepository;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.VoucherRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantVoucherService {
    private static final Set<String> DISCOUNT_TYPES =
            Set.of("PERCENTAGE", "FIXED_AMOUNT");

    private static final Set<String> VOUCHER_STATUSES =
            Set.of("DRAFT", "ACTIVE", "EXPIRED", "DISABLED");

    private final VoucherRepository voucherRepository;
    private final MerchantRepository merchantRepository;
    private final CampaignApplicationRepository applicationRepository;

    @Transactional
    public VoucherDtos.PageResponse list(
            int page,
            int size,
            String search,
            String status,
            UUID campaignApplicationId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);

        // Cập nhật trạng thái hết hạn khi Merchant đọc danh sách.
        voucherRepository.expirePastDueForMerchant(merchant.getId());

        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.length() > 200) {
            throw new IllegalArgumentException(
                    "search must not exceed 200 characters"
            );
        }

        String normalizedStatus = normalizeStatus(status);
        if (!normalizedStatus.isEmpty()
                && !VOUCHER_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid voucher status");
        }

        Specification<Voucher> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(
                    root.get("merchant").get("id"),
                    merchant.getId()
            ));

            if (campaignApplicationId != null) {
                predicates.add(cb.equal(
                        root.get("campaignApplication").get("id"),
                        campaignApplicationId
                ));
            }

            if (!normalizedStatus.isEmpty()) {
                predicates.add(cb.equal(
                        root.get("status"),
                        normalizedStatus
                ));
            }

            if (!normalizedSearch.isEmpty()) {
                String pattern = "%"
                        + escapeLike(normalizedSearch.toLowerCase(Locale.ROOT))
                        + "%";

                predicates.add(cb.or(
                        cb.like(
                                cb.lower(root.<String>get("code")),
                                pattern,
                                '\\'
                        ),
                        cb.like(
                                cb.lower(root.<String>get("name")),
                                pattern,
                                '\\'
                        )
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Voucher> result = voucherRepository.findMerchantVouchers(
                specification,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.asc("id")
                        )
                )
        );

        return new VoucherDtos.PageResponse(
                result.getContent().stream()
                        .map(this::toResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public VoucherDtos.VoucherResponse get(
            UUID voucherId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        voucherRepository.expirePastDueForMerchant(merchant.getId());

        return toResponse(findOwnedVoucher(merchant.getId(), voucherId));
    }

    @Transactional
    public VoucherDtos.VoucherResponse create(
            VoucherDtos.UpsertRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validateRequest(request, 0L);

        String code = normalizeCode(request.code());
        ensureCodeAvailable(merchant.getId(), code, null);

        Voucher voucher = new Voucher();
        voucher.setMerchant(merchant);
        voucher.setUsageCount(0L);
        voucher.setStatus("DRAFT");

        applyRequest(voucher, request, merchant);
        return toResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherDtos.VoucherResponse update(
            UUID voucherId,
            VoucherDtos.UpsertRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        voucherRepository.expirePastDueForMerchant(merchant.getId());

        Voucher voucher = findOwnedVoucher(merchant.getId(), voucherId);

        if (!"DRAFT".equals(voucher.getStatus())) {
            throw new IllegalStateException(
                    "Only draft vouchers can be edited"
            );
        }

        validateRequest(request, voucher.getUsageCount());

        String code = normalizeCode(request.code());
        ensureCodeAvailable(merchant.getId(), code, voucherId);

        applyRequest(voucher, request, merchant);
        return toResponse(voucherRepository.save(voucher));
    }

    @Transactional
    public VoucherDtos.VoucherResponse changeStatus(
            UUID voucherId,
            VoucherDtos.StatusRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        voucherRepository.expirePastDueForMerchant(merchant.getId());

        if (request == null || request.status() == null
                || request.status().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        String targetStatus = normalizeStatus(request.status());
        if (!Set.of("ACTIVE", "DISABLED").contains(targetStatus)) {
            throw new IllegalArgumentException(
                    "status must be ACTIVE or DISABLED"
            );
        }

        Voucher voucher = findOwnedVoucher(merchant.getId(), voucherId);
        String currentStatus = voucher.getStatus();

        if (currentStatus.equals(targetStatus)) {
            return toResponse(voucher);
        }

        if ("EXPIRED".equals(currentStatus)) {
            throw new IllegalStateException(
                    "Expired vouchers cannot change status"
            );
        }

        boolean allowed = switch (currentStatus) {
            case "DRAFT" ->
                    "ACTIVE".equals(targetStatus)
                            || "DISABLED".equals(targetStatus);
            case "ACTIVE" -> "DISABLED".equals(targetStatus);
            case "DISABLED" -> "ACTIVE".equals(targetStatus);
            default -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Voucher cannot change from "
                            + currentStatus + " to " + targetStatus
            );
        }

        if ("ACTIVE".equals(targetStatus)
                && voucher.getExpiresAt() != null
                && !voucher.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalStateException(
                    "Expired vouchers cannot be activated"
            );
        }

        voucher.setStatus(targetStatus);
        return toResponse(voucherRepository.save(voucher));
    }

    private void applyRequest(
            Voucher voucher,
            VoucherDtos.UpsertRequest request,
            Merchant merchant
    ) {
        voucher.setCampaignApplication(
                resolveApplication(
                        request.campaignApplicationId(),
                        merchant.getId()
                )
        );

        voucher.setCode(normalizeCode(request.code()));
        voucher.setName(request.name().trim());
        voucher.setDescription(normalizeOptional(request.description()));
        voucher.setDiscountType(request.discountType());
        voucher.setDiscountValue(request.discountValue());
        voucher.setMaxDiscountAmount(request.maxDiscountAmount());
        voucher.setMinOrderAmount(request.minOrderAmount());
        voucher.setUsageLimit(request.usageLimit());
        voucher.setPerCustomerLimit(request.perCustomerLimit());
        voucher.setStartsAt(request.startsAt());
        voucher.setExpiresAt(request.expiresAt());
    }

    private void validateRequest(
            VoucherDtos.UpsertRequest request,
            long currentUsageCount
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("code is required");
        }

        if (!request.code().matches("[A-Za-z0-9_-]{2,100}")) {
            throw new IllegalArgumentException(
                    "code may contain only letters, digits, underscore, or hyphen"
            );
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        if (!DISCOUNT_TYPES.contains(request.discountType())) {
            throw new IllegalArgumentException("Invalid discountType");
        }

        if (request.discountValue() == null
                || request.discountValue().signum() < 0) {
            throw new IllegalArgumentException(
                    "discountValue must be zero or greater"
            );
        }

        if ("PERCENTAGE".equals(request.discountType())
                && request.discountValue().compareTo(
                        new BigDecimal("100")
                ) > 0) {
            throw new IllegalArgumentException(
                    "Percentage discount cannot exceed 100"
            );
        }

        if (request.maxDiscountAmount() != null
                && request.maxDiscountAmount().signum() < 0) {
            throw new IllegalArgumentException(
                    "maxDiscountAmount must be zero or greater"
            );
        }

        if (request.minOrderAmount() == null
                || request.minOrderAmount().signum() < 0) {
            throw new IllegalArgumentException(
                    "minOrderAmount must be zero or greater"
            );
        }

        if (request.usageLimit() != null
                && request.usageLimit() < currentUsageCount) {
            throw new IllegalArgumentException(
                    "usageLimit cannot be lower than usageCount"
            );
        }

        if (request.perCustomerLimit() != null
                && request.perCustomerLimit() <= 0) {
            throw new IllegalArgumentException(
                    "perCustomerLimit must be greater than zero"
            );
        }

        if (request.startsAt() != null
                && request.expiresAt() != null
                && !request.expiresAt().isAfter(request.startsAt())) {
            throw new IllegalArgumentException(
                    "expiresAt must be later than startsAt"
            );
        }

        if (request.expiresAt() != null
                && !request.expiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "expiresAt must be in the future"
            );
        }
    }

    private CampaignApplication resolveApplication(
            UUID applicationId,
            UUID merchantId
    ) {
        if (applicationId == null) {
            return null;
        }

        CampaignApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign application not found"
                ));

        UUID applicationMerchantId = application.getCampaign()
                .getProduct()
                .getMerchant()
                .getId();

        if (!merchantId.equals(applicationMerchantId)) {
            throw new ResourceNotFoundException(
                    "Campaign application not found"
            );
        }

        if (!"APPROVED".equals(application.getStatus())) {
            throw new IllegalStateException(
                    "Voucher can only be assigned to an approved campaign application"
            );
        }

        return application;
    }

    private void ensureCodeAvailable(
            UUID merchantId,
            String code,
            UUID currentVoucherId
    ) {
        boolean exists = currentVoucherId == null
                ? voucherRepository.existsByMerchant_IdAndCodeIgnoreCase(
                        merchantId,
                        code
                )
                : voucherRepository
                        .existsByMerchant_IdAndCodeIgnoreCaseAndIdNot(
                                merchantId,
                                code,
                                currentVoucherId
                        );

        if (exists) {
            throw new IllegalStateException(
                    "Voucher code already exists for this merchant"
            );
        }
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

    private Voucher findOwnedVoucher(UUID merchantId, UUID voucherId) {
        return voucherRepository.findByMerchant_IdAndId(
                        merchantId,
                        voucherId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Voucher not found"
                ));
    }

    private VoucherDtos.VoucherResponse toResponse(Voucher voucher) {
        UUID applicationId = voucher.getCampaignApplication() == null
                ? null
                : voucher.getCampaignApplication().getId();

        return new VoucherDtos.VoucherResponse(
                voucher.getId(),
                applicationId,
                voucher.getCode(),
                voucher.getName(),
                voucher.getDescription(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                voucher.getMaxDiscountAmount(),
                voucher.getMinOrderAmount(),
                voucher.getUsageLimit(),
                voucher.getUsageCount(),
                voucher.getPerCustomerLimit(),
                voucher.getStartsAt(),
                voucher.getExpiresAt(),
                voucher.getStatus(),
                voucher.getCreatedAt(),
                voucher.getUpdatedAt()
        );
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        return status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
}