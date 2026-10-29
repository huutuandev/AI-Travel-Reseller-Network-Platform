package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.CreateProductRequest;
import com.aitravel.reseller.dto.request.UpdateProductRequest;
import com.aitravel.reseller.dto.respone.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    /**
     * Tạo sản phẩm mới gắn với Merchant của user đang đăng nhập.
     */
    ProductResponse createProduct(CreateProductRequest request);

    /**
     * Lấy chi tiết sản phẩm theo ID.
     */
    ProductResponse getProductById(UUID id);

    /**
     * Lấy danh sách sản phẩm theo Merchant ID có phân trang.
     */
    Page<ProductResponse> getProductsByMerchant(UUID merchantId, Pageable pageable);

    /**
     * Lấy danh sách sản phẩm của chính Merchant đang đăng nhập có phân trang.
     */
    Page<ProductResponse> getMyProducts(Pageable pageable);

    /**
     * Cập nhật sản phẩm (chỉ chủ sở hữu sản phẩm mới được cập nhật).
     */
    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    /**
     * Lấy danh sách sản phẩm công khai (status = ACTIVE) có phân trang.
     */
    Page<ProductResponse> getPublicProducts(Pageable pageable);

    /**
     * Xóa sản phẩm (soft delete chuyển status sang ARCHIVED, chỉ chủ sở hữu mới được xóa).
     */
    void deleteProduct(UUID id);
}
