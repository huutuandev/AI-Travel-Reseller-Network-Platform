package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.ProductDtos;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.ProductRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantProductService {
    private static final Set<String> STATUSES =
            Set.of("AVAILABLE", "UNAVAILABLE", "SOLD_OUT", "DRAFT");

    private static final Pattern NON_SLUG_CHARACTERS =
            Pattern.compile("[^a-z0-9]+");

    private final ProductRepository productRepository;
    private final MerchantRepository merchantRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ProductDtos.ProductResponse create(
            ProductDtos.UpsertRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validateRequest(request);

        Product product = Product.builder()
                .merchant(merchant)
                .title(request.title().trim())
                .slug(createSlug(merchant.getId(), request.title()))
                .description(normalizeOptional(request.description()))
                .categoryCode(normalizeOptional(request.categoryCode()))
                .destinationCode(normalizeOptional(request.destinationCode()))
                .images(writeImages(request.images()))
                .price(request.price())
                .salePrice(request.salePrice())
                .currency(request.currency())
                .availabilityStatus(request.availabilityStatus())
                .officialCheckoutUrl(validateCheckoutUrl(request.officialCheckoutUrl()))
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDtos.ProductResponse update(
            UUID productId,
            ProductDtos.UpsertRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validateRequest(request);

        Product product = findOwnedProduct(merchant.getId(), productId);
        product.setTitle(request.title().trim());
        product.setDescription(normalizeOptional(request.description()));
        product.setCategoryCode(normalizeOptional(request.categoryCode()));
        product.setDestinationCode(normalizeOptional(request.destinationCode()));
        product.setImages(writeImages(request.images()));
        product.setPrice(request.price());
        product.setSalePrice(request.salePrice());
        product.setCurrency(request.currency());
        product.setAvailabilityStatus(request.availabilityStatus());
        product.setOfficialCheckoutUrl(validateCheckoutUrl(request.officialCheckoutUrl()));

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDtos.ProductResponse changeAvailability(
            UUID productId,
            ProductDtos.AvailabilityRequest request,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        if (request == null || !STATUSES.contains(request.availabilityStatus())) {
            throw new IllegalArgumentException("Invalid product availability status");
        }

        Product product = findOwnedProduct(merchant.getId(), productId);
        product.setAvailabilityStatus(request.availabilityStatus());

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDtos.ProductResponse get(UUID productId, CustomUserDetails user) {
        Merchant merchant = requireActiveMerchant(user);
        return toResponse(findOwnedProduct(merchant.getId(), productId));
    }

    @Transactional
    public ProductDtos.ProductPageResponse list(
            int page,
            int size,
            String search,
            String status,
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

        String normalizedStatus = status == null ? "" : status.trim();
        if (!normalizedStatus.isEmpty() && !STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                    "Invalid product availability status"
            );
        }

        Page<Product> result = productRepository.searchForMerchant(
                merchant.getId(),
                normalizedSearch,
                normalizedStatus,
                PageRequest.of(page, size, Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.asc("id")
                ))
        );

        return new ProductDtos.ProductPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
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
            throw new AccessDeniedException("Merchant account is not active");
        }

        return merchant;
    }

    private Product findOwnedProduct(UUID merchantId, UUID productId) {
        return productRepository.findByMerchantIdAndId(merchantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void validateRequest(ProductDtos.UpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        if (!STATUSES.contains(request.availabilityStatus())) {
            throw new IllegalArgumentException("Invalid product availability status");
        }

        if (request.salePrice() != null
                && request.salePrice().compareTo(request.price()) > 0) {
            throw new IllegalArgumentException("salePrice must not exceed price");
        }

        validateImages(request.images());
        validateCheckoutUrl(request.officialCheckoutUrl());
    }

    private void validateImages(List<Object> images) {
        if (images == null) {
            throw new IllegalArgumentException("images must be a JSON array");
        }
        if (images.size() > 20) {
            throw new IllegalArgumentException("images must contain at most 20 items");
        }

        for (Object image : images) {
            String url = null;

            if (image instanceof String value) {
                url = value;
            } else if (image instanceof Map<?, ?> map
                    && map.get("url") instanceof String value) {
                url = value;
            }

            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException(
                        "Each image must be a URL string or an object with a url field"
                );
            }

            validateHttpUrl(url, "image URL");
        }
    }

    private String validateCheckoutUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("officialCheckoutUrl is required");
        }

        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException(
                        "officialCheckoutUrl must be a valid HTTPS URL"
                );
            }
            return uri.toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "officialCheckoutUrl must be a valid HTTPS URL"
            );
        }
    }

    private void validateHttpUrl(String value, String fieldName) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (uri.getHost() == null
                    || scheme == null
                    || (!"https".equalsIgnoreCase(scheme)
                    && !"http".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException(fieldName + " must be a valid HTTP(S) URL");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid HTTP(S) URL");
        }
    }

    private String writeImages(List<Object> images) {
        try {
            return objectMapper.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("images contain invalid JSON");
        }
    }

    private List<Object> readImages(String value) {
        try {
            return objectMapper.readValue(
                    value,
                    new TypeReference<List<Object>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Stored product images contain invalid JSON",
                    e
            );
        }
    }

    private ProductDtos.ProductResponse toResponse(Product product) {
        return new ProductDtos.ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getSlug(),
                product.getDescription(),
                product.getCategoryCode(),
                product.getDestinationCode(),
                readImages(product.getImages()),
                product.getPrice(),
                product.getSalePrice(),
                product.getCurrency(),
                product.getAvailabilityStatus(),
                product.getOfficialCheckoutUrl(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private String createSlug(UUID merchantId, String title) {
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        String base = NON_SLUG_CHARACTERS.matcher(normalized)
                .replaceAll("-")
                .replaceAll("(^-+|-+$)", "");

        if (base.isBlank()) {
            base = "product";
        }
        if (base.length() > 220) {
            base = base.substring(0, 220).replaceAll("-+$", "");
        }

        String candidate = base;
        int suffix = 2;
        while (productRepository.existsByMerchantIdAndSlug(merchantId, candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}