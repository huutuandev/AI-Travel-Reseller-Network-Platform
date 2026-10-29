package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.request.RegisterMerchantRequest;
import com.aitravel.reseller.dto.request.UpdateMerchantRequest;
import com.aitravel.reseller.dto.respone.MerchantResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MerchantResponse registerMerchant(RegisterMerchantRequest request) {
        UUID userId = getCurrentUserId();
        if (merchantRepository.existsByUser_Id(userId)) {
            throw new IllegalStateException("User already has a registered merchant profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Merchant merchant = Merchant.builder()
                .user(user)
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .website(request.getWebsite())
                .logoUrl(request.getLogoUrl())
                .verificationStatus("PENDING")
                .build();

        Merchant saved = merchantRepository.save(merchant);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantResponse getCurrentMerchant() {
        UUID userId = getCurrentUserId();
        Merchant merchant = merchantRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant profile not found for the current user"));
        return mapToResponse(merchant);
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantResponse getMerchantById(UUID id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));
        return mapToResponse(merchant);
    }

    @Override
    @Transactional
    public MerchantResponse updateCurrentMerchant(UpdateMerchantRequest request) {
        Merchant merchant = getCurrentMerchantEntity();

        merchant.setName(request.getName());
        merchant.setLogoUrl(request.getLogoUrl());
        merchant.setPhone(request.getPhone());
        merchant.setAddress(request.getAddress());
        merchant.setWebsite(request.getWebsite());

        Merchant saved = merchantRepository.save(merchant);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return merchantRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateOwnership(UUID merchantId) {
        Merchant current = getCurrentMerchantEntity();
        if (!current.getId().equals(merchantId)) {
            throw new AccessDeniedException("You do not have permission to access or modify this merchant's data");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Merchant getCurrentMerchantEntity() {
        UUID userId = getCurrentUserId();
        return merchantRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AccessDeniedException("User does not have an onboarded merchant profile"));
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        throw new AccessDeniedException("Invalid authentication principal");
    }

    private MerchantResponse mapToResponse(Merchant merchant) {
        return MerchantResponse.builder()
                .id(merchant.getId())
                .userId(merchant.getUser() != null ? merchant.getUser().getId() : null)
                .name(merchant.getName())
                .logoUrl(merchant.getLogoUrl())
                .phone(merchant.getPhone())
                .address(merchant.getAddress())
                .website(merchant.getWebsite())
                .verificationStatus(merchant.getVerificationStatus())
                .createdAt(merchant.getCreatedAt())
                .build();
    }
}
