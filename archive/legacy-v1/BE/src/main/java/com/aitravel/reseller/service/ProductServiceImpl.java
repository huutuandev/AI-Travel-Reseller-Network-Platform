package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.CreateProductRequest;
import com.aitravel.reseller.dto.request.UpdateProductRequest;
import com.aitravel.reseller.dto.respone.ProductResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final MerchantService merchantService;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        validatePrices(request.getOriginalPrice(), request.getSalePrice());

        // Từ chối nếu client tự chỉ định status
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Client cannot specify product status. New products are automatically created with ACTIVE status.");
        }

        Merchant merchant = merchantService.getCurrentMerchantEntity();
        if ("REJECTED".equalsIgnoreCase(merchant.getVerificationStatus())) {
            throw new AccessDeniedException("Merchant verification status is REJECTED. Cannot create products.");
        }

        Product product = Product.builder()
                .merchant(merchant)
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .originalPrice(request.getOriginalPrice())
                .salePrice(request.getSalePrice())
                .bookingUrl(request.getBookingUrl())
                .faqStructured(request.getFaqStructured())
                .mediaAssets(request.getMediaAssets())
                .status("ACTIVE")
                .build();

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Đảm bảo chỉ sản phẩm ACTIVE mới hiển thị công khai.
        // Merchant sở hữu hoặc ADMIN vẫn được xem sản phẩm non-active (DRAFT/ARCHIVED).
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            boolean isAuthorized = false;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                if (isAdmin) {
                    isAuthorized = true;
                } else {
                    try {
                        Merchant currentMerchant = merchantService.getCurrentMerchantEntity();
                        if (product.getMerchant() != null && product.getMerchant().getId().equals(currentMerchant.getId())) {
                            isAuthorized = true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            if (!isAuthorized) {
                throw new ResourceNotFoundException("Product not found with id: " + id);
            }
        }

        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByMerchant(UUID merchantId, Pageable pageable) {
        if (!merchantService.existsById(merchantId)) {
            throw new ResourceNotFoundException("Merchant not found with id: " + merchantId);
        }
        return productRepository.findByMerchant_Id(merchantId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getMyProducts(Pageable pageable) {
        Merchant currentMerchant = merchantService.getCurrentMerchantEntity();
        return productRepository.findByMerchant_Id(currentMerchant.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Merchant currentMerchant = merchantService.getCurrentMerchantEntity();
        if (!product.getMerchant().getId().equals(currentMerchant.getId())) {
            throw new AccessDeniedException("You do not have permission to update this product");
        }

        // Ngăn chặn cập nhật sản phẩm đã bị ARCHIVED
        if ("ARCHIVED".equalsIgnoreCase(product.getStatus())) {
            throw new IllegalStateException("Cannot update an ARCHIVED product.");
        }

        // Từ chối nếu client tự chỉ định status trong update request
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Client cannot specify product status directly.");
        }

        validatePrices(request.getOriginalPrice(), request.getSalePrice());

        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setDescription(request.getDescription());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setSalePrice(request.getSalePrice());
        product.setBookingUrl(request.getBookingUrl());
        product.setFaqStructured(request.getFaqStructured());
        product.setMediaAssets(request.getMediaAssets());

        // Merchant cập nhật trực tiếp sản phẩm của mình; giữ nguyên trạng thái ACTIVE
        product.setStatus("ACTIVE");

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Merchant currentMerchant = merchantService.getCurrentMerchantEntity();
        if (!product.getMerchant().getId().equals(currentMerchant.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this product");
        }

        // Ưu tiên soft delete bằng cách chuyển trạng thái sang ARCHIVED
        product.setStatus("ARCHIVED");
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(Pageable pageable) {
        return productRepository.findByStatus("ACTIVE", pageable)
                .map(this::mapToResponse);
    }

    private void validatePrices(BigDecimal originalPrice, BigDecimal salePrice) {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sale price must be greater than 0");
        }
        if (originalPrice != null && originalPrice.compareTo(salePrice) < 0) {
            throw new IllegalArgumentException("Original price must be greater than or equal to sale price");
        }
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .merchantId(product.getMerchant() != null ? product.getMerchant().getId() : null)
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .originalPrice(product.getOriginalPrice())
                .salePrice(product.getSalePrice())
                .bookingUrl(product.getBookingUrl())
                .faqStructured(product.getFaqStructured())
                .mediaAssets(product.getMediaAssets())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
