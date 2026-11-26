package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.RegisterMerchantRequest;
import com.aitravel.reseller.dto.request.UpdateMerchantRequest;
import com.aitravel.reseller.dto.respone.MerchantResponse;
import com.aitravel.reseller.entity.Merchant;

import java.util.UUID;

public interface MerchantService {

    /**
     * Đăng ký hồ sơ Merchant cho User đang đăng nhập.
     */
    MerchantResponse registerMerchant(RegisterMerchantRequest request);

    /**
     * Lấy thông tin Merchant của User đang đăng nhập dựa trên token JWT.
     */
    MerchantResponse getCurrentMerchant();

    /**
     * Lấy chi tiết Merchant theo ID.
     */
    MerchantResponse getMerchantById(UUID id);

    /**
     * Cập nhật thông tin Merchant thuộc tài khoản đang đăng nhập.
     */
    MerchantResponse updateCurrentMerchant(UpdateMerchantRequest request);

    /**
     * Kiểm tra Merchant có tồn tại theo ID hay không.
     */
    boolean existsById(UUID id);

    /**
     * Kiểm tra quyền sở hữu Merchant của User đang đăng nhập.
     * Ném AccessDeniedException nếu không có quyền.
     */
    void validateOwnership(UUID merchantId);

    /**
     * Lấy JPA Entity của Merchant đang đăng nhập (dùng nội bộ cho các Service khác).
     */
    Merchant getCurrentMerchantEntity();
}
