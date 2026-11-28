package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.CampaignDtos;
import com.aitravel.reseller.entity.Campaign;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.CampaignApplicationRepository;
import com.aitravel.reseller.repository.CampaignRepository;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.ProductRepository;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantCampaignService {
    private static final Set<String> COMMISSION_TYPES =
            Set.of("PERCENTAGE", "FIXED_AMOUNT");

    private static final Set<String> SUPPORTED_COMMISSION_CURRENCIES = Set.of(
            "AUD", "CAD", "CHF", "CNY", "EUR", "GBP", "HKD", "IDR",
            "INR", "JPY", "KRW", "MYR", "NZD", "PHP", "SGD", "THB",
            "TWD", "USD", "VND"
    );

    private static final Set<String> CAMPAIGN_STATUSES =
            Set.of(
                    "DRAFT",
                    "PENDING_REVIEW",
                    "ACTIVE",
                    "PAUSED",
                    "ENDED",
                    "CANCELLED"
            );

    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public CampaignDtos.CampaignPageResponse list(
            int page,
            int size,
            String search,
            String status,
            UUID productId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.length() > 200) {
            throw new IllegalArgumentException(
                    "search must not exceed 200 characters"
            );
        }

        String normalizedStatus = status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);

        if (!normalizedStatus.isEmpty()
                && !CAMPAIGN_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid campaign status");
        }

        Specification<Campaign> specification = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            predicates.add(cb.equal(
                    root.get("product").get("merchant").get("id"),
                    merchant.getId()
            ));

            if (productId != null) {
                predicates.add(cb.equal(
                        root.get("product").get("id"),
                        productId
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
                                cb.lower(root.get("title")),
                                pattern,
                                '\\'
                        ),
                        cb.like(
                                cb.lower(root.get("product").get("title")),
                                pattern,
                                '\\'
                        )
                ));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Campaign> result = campaignRepository.findAll(
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

        return new CampaignDtos.CampaignPageResponse(
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
    public CampaignDtos.CampaignResponse create(
            CampaignDtos.UpsertRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validateRequest(request);

        Product product = findOwnedProduct(merchant.getId(), request.productId());
        String availabilityStatus = product.getAvailabilityStatus();

        if (availabilityStatus != null
                && "SOLD_OUT".equalsIgnoreCase(availabilityStatus.trim())) {
                throw new IllegalStateException(
                        "A campaign cannot be created for a sold-out product"
                );
        }
        Campaign campaign = new Campaign();

        applyRequest(campaign, request, product);
        campaign.setStatus("DRAFT");
        campaign.setTermsVersion(1);

        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public CampaignDtos.CampaignResponse get(
            UUID campaignId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        return toResponse(findOwnedCampaign(merchant.getId(), campaignId));
    }

    @Transactional
    public CampaignDtos.CampaignResponse update(
            UUID campaignId,
            CampaignDtos.UpsertRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validateRequest(request);

        Campaign campaign = findOwnedCampaign(merchant.getId(), campaignId);

        if ("ENDED".equals(campaign.getStatus())
                || "CANCELLED".equals(campaign.getStatus())) {
            throw new IllegalStateException(
                    "Ended or cancelled campaigns cannot be updated"
            );
        }

        boolean hasApplications =
                applicationRepository.existsByCampaign_Id(campaign.getId());

        Product newProduct = findOwnedProduct(
                merchant.getId(),
                request.productId()
        );

        if (hasApplications) {
            if (!Objects.equals(
                    campaign.getProduct().getId(),
                    newProduct.getId()
            )) {
                throw new IllegalStateException(
                        "The product cannot be changed after resellers apply"
                );
            }

            if (!Objects.equals(
                    normalizeOptional(campaign.getTermsText()),
                    normalizeOptional(request.termsText())
            )) {
                throw new IllegalStateException(
                        "Campaign terms cannot be changed after resellers apply"
                );
            }

            if (!Objects.equals(
                    campaign.getCommissionType(),
                    request.commissionType()
            )) {
                throw new IllegalStateException(
                        "Commission type cannot be changed after resellers apply"
                );
            }

            if (!Objects.equals(
                    campaign.getCommissionCurrency(),
                    request.commissionCurrency()
            )) {
                throw new IllegalStateException(
                        "Commission currency cannot be changed after resellers apply"
                );
            }
        }

        if ("ACTIVE".equals(campaign.getStatus())
                && !"AVAILABLE".equals(newProduct.getAvailabilityStatus())) {
            throw new IllegalStateException(
                    "An active campaign must use an available product"
            );
        }

        if ("ACTIVE".equals(campaign.getStatus())
                && request.endDate() != null
                && !request.endDate().isAfter(Instant.now())) {
            throw new IllegalStateException(
                    "Use the status endpoint to end an active campaign"
            );
        }

        boolean termsChanged = !Objects.equals(
                normalizeOptional(campaign.getTermsText()),
                normalizeOptional(request.termsText())
        );

        applyRequest(campaign, request, newProduct);

        if (termsChanged) {
            campaign.setTermsVersion(campaign.getTermsVersion() + 1);
        }

        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public CampaignDtos.CampaignResponse changeStatus(
            UUID campaignId,
            CampaignDtos.StatusRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        String targetStatus = request.status();
        Campaign campaign = findOwnedCampaign(merchant.getId(), campaignId);
        String currentStatus = campaign.getStatus();

        if (currentStatus.equals(targetStatus)) {
            return toResponse(campaign);
        }

        switch (targetStatus) {
            case "ACTIVE" -> activate(campaign);
            case "PAUSED" -> {
                if (!"ACTIVE".equals(currentStatus)) {
                    throw new IllegalStateException(
                            "Only active campaigns can be paused"
                    );
                }
                campaign.setStatus("PAUSED");
            }
            case "ENDED" -> {
                if (!"ACTIVE".equals(currentStatus)
                        && !"PAUSED".equals(currentStatus)) {
                    throw new IllegalStateException(
                            "Only active or paused campaigns can be ended"
                    );
                }
                campaign.setStatus("ENDED");
            }
            case "CANCELLED" -> {
                if (!"DRAFT".equals(currentStatus)
                        && !"ACTIVE".equals(currentStatus)
                        && !"PAUSED".equals(currentStatus)) {
                    throw new IllegalStateException(
                            "This campaign cannot be cancelled from its current state"
                    );
                }
                campaign.setStatus("CANCELLED");
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported campaign status"
            );
        }

        return toResponse(campaignRepository.save(campaign));
    }

    private void activate(Campaign campaign) {
        String currentStatus = campaign.getStatus();

        if (!"DRAFT".equals(currentStatus)
                && !"PAUSED".equals(currentStatus)) {
            throw new IllegalStateException(
                    "Only draft or paused campaigns can be activated"
            );
        }

        if (!"AVAILABLE".equals(
                campaign.getProduct().getAvailabilityStatus()
        )) {
            throw new IllegalStateException(
                    "Campaign product must be available before activation"
            );
        }

        if (campaign.getEndDate() != null
                && !campaign.getEndDate().isAfter(Instant.now())) {
            throw new IllegalStateException(
                    "An expired campaign cannot be activated"
            );
        }

        campaign.setStatus("ACTIVE");
    }

    private void validateRequest(CampaignDtos.UpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (request.productId() == null) {
            throw new IllegalArgumentException("productId is required");
        }

        if (!COMMISSION_TYPES.contains(request.commissionType())) {
            throw new IllegalArgumentException("Invalid commissionType");
        }

        if (request.commissionRate() == null
                || request.commissionRate().signum() < 0) {
            throw new IllegalArgumentException(
                    "commissionRate must be zero or greater"
            );
        }

        if ("PERCENTAGE".equals(request.commissionType())) {
            if (request.commissionRate().compareTo(
                    new java.math.BigDecimal("100")
            ) > 0) {
                throw new IllegalArgumentException(
                        "Percentage commission cannot exceed 100"
                );
            }
            if (request.commissionCurrency() != null
                    && !request.commissionCurrency().isBlank()) {
                throw new IllegalArgumentException(
                        "commissionCurrency must be omitted for percentage commission"
                );
            }
        } else {
            if (request.commissionCurrency() == null
                    || !SUPPORTED_COMMISSION_CURRENCIES.contains(
                            request.commissionCurrency()
                    )) {
                throw new IllegalArgumentException(
                        "Fixed commission currency must be one of the supported currencies: "
                                + String.join(
                                        ", ",
                                        SUPPORTED_COMMISSION_CURRENCIES.stream()
                                                .sorted()
                                                .toList()
                                )
                );
            }
        }

        if (request.cookieWindowDays() == null
                || request.cookieWindowDays() < 1
                || request.cookieWindowDays() > 3650) {
            throw new IllegalArgumentException(
                    "cookieWindowDays must be between 1 and 3650"
            );
        }

        if (request.startDate() != null
                && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException(
                    "endDate must not be before startDate"
            );
        }
    }

    private void applyRequest(
            Campaign campaign,
            CampaignDtos.UpsertRequest request,
            Product product
    ) {
        campaign.setProduct(product);
        campaign.setTitle(request.title().trim());
        campaign.setDescription(normalizeOptional(request.description()));
        campaign.setCommissionType(request.commissionType());
        campaign.setCommissionRate(request.commissionRate());
        campaign.setCommissionCurrency(
                "PERCENTAGE".equals(request.commissionType())
                        ? null
                        : request.commissionCurrency()
        );
        campaign.setCookieWindowDays(request.cookieWindowDays());
        campaign.setTermsText(normalizeOptional(request.termsText()));
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
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
            throw new AccessDeniedException("Merchant account is not active");
        }

        return merchant;
    }

    private Product findOwnedProduct(UUID merchantId, UUID productId) {
        return productRepository.findByMerchantIdAndId(merchantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"
                ));
    }

    private Campaign findOwnedCampaign(UUID merchantId, UUID campaignId) {
        return campaignRepository
                .findByIdAndProduct_Merchant_Id(campaignId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campaign not found"
                ));
    }

    private CampaignDtos.CampaignResponse toResponse(Campaign campaign) {
        Product product = campaign.getProduct();

        return new CampaignDtos.CampaignResponse(
                campaign.getId(),
                new CampaignDtos.ProductSummary(
                        product.getId(),
                        product.getTitle(),
                        product.getPrice(),
                        product.getCurrency(),
                        product.getAvailabilityStatus()
                ),
                campaign.getTitle(),
                campaign.getDescription(),
                campaign.getCommissionType(),
                campaign.getCommissionRate(),
                campaign.getCommissionCurrency(),
                campaign.getCookieWindowDays(),
                campaign.getTermsText(),
                campaign.getTermsVersion(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
