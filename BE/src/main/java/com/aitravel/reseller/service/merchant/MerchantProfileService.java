package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.UpdateMerchantBrandingRequest;
import com.aitravel.reseller.dto.request.merchant.UpdateMerchantProfileRequest;
import com.aitravel.reseller.dto.respone.merchant.MerchantBrandingResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantProfileResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.MerchantBranding;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantBrandingRepository;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MerchantProfileService {

    private final MerchantRepository merchantRepository;
    private final MerchantBrandingRepository brandingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MerchantProfileResponse getMyProfile() {
        Merchant merchant = currentMerchant(false);
        User owner = merchant.getUser();
        MerchantBranding branding = brandingRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> defaultBrandingObject(merchant));

        return MerchantOnboardingService.toProfileResponse(merchant, owner, branding);
    }

    @Transactional
    public MerchantProfileResponse updateMyProfile(UpdateMerchantProfileRequest request) {
        Merchant merchant = currentMerchant(true);
        User owner = merchant.getUser();

        String customDomain = MerchantOnboardingService.normalizeAndValidateCustomDomain(request.getCustomDomain());
        if (customDomain != null) {
            Optional<Merchant> existingWithDomain = merchantRepository.findByCustomDomainIgnoreCase(customDomain);
            if (existingWithDomain.isPresent() && !existingWithDomain.get().getId().equals(merchant.getId())) {
                throw new IllegalStateException("Official website domain '" + customDomain + "' is already linked to another merchant");
            }
        }

        merchant.setName(request.getBusinessName().trim());
        merchant.setDescription(trimToNull(request.getDescription()));
        merchant.setContactEmail(request.getContactEmail().trim().toLowerCase(Locale.ROOT));
        merchant.setPhone(trimToNull(request.getPhone()));
        merchant.setAddress(trimToNull(request.getAddress()));
        merchant.setCustomDomain(customDomain);

        if (owner != null) {
            boolean ownerChanged = false;
            if (request.getOwnerFullName() != null && !request.getOwnerFullName().isBlank()) {
                owner.setFullName(request.getOwnerFullName().trim());
                ownerChanged = true;
            }
            if (request.getOwnerPhone() != null) {
                owner.setPhone(trimToNull(request.getOwnerPhone()));
                ownerChanged = true;
            }
            if (ownerChanged) {
                owner = userRepository.save(owner);
            }
        }

        merchant = merchantRepository.save(merchant);
        Merchant finalMerchant = merchant;
        MerchantBranding branding = brandingRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> brandingRepository.save(defaultBrandingObject(finalMerchant)));

        return MerchantOnboardingService.toProfileResponse(merchant, owner, branding);
    }

    @Transactional(readOnly = true)
    public MerchantBrandingResponse getMyBranding() {
        Merchant merchant = currentMerchant(false);
        MerchantBranding branding = brandingRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> defaultBrandingObject(merchant));
        return toBrandingResponse(merchant, branding);
    }

    @Transactional
    public MerchantBrandingResponse updateMyBranding(UpdateMerchantBrandingRequest request) {
        Merchant merchant = currentMerchant(true);
        MerchantBranding branding = brandingRepository.findByMerchantId(merchant.getId())
                .orElseGet(() -> defaultBrandingObject(merchant));

        branding.setLogoUrl(validateOptionalUrl(request.getLogoUrl(), "logoUrl"));
        branding.setFaviconUrl(validateOptionalUrl(request.getFaviconUrl(), "faviconUrl"));

        if (request.getPrimaryColor() != null && !request.getPrimaryColor().isBlank()) {
            branding.setPrimaryColor(request.getPrimaryColor().trim().toUpperCase(Locale.ROOT));
        }
        if (request.getSecondaryColor() != null && !request.getSecondaryColor().isBlank()) {
            branding.setSecondaryColor(request.getSecondaryColor().trim().toUpperCase(Locale.ROOT));
        }

        branding.setCustomCss(trimToNull(request.getCustomCss()));
        branding.setFooterText(trimToNull(request.getFooterText()));

        branding = brandingRepository.save(branding);
        return toBrandingResponse(merchant, branding);
    }

    private Merchant currentMerchant(boolean requireActiveStatus) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }

        boolean hasMerchantRole = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MERCHANT".equals(a.getAuthority()));
        if (!user.isEnabled() || !hasMerchantRole) {
            throw new AccessDeniedException("Merchant role required");
        }

        Merchant merchant = merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant profile not found for this account"));

        if (requireActiveStatus && !"ACTIVE".equals(merchant.getStatus())) {
            throw new AccessDeniedException("Merchant workspace is currently " + merchant.getStatus() + " and cannot be modified");
        }

        return merchant;
    }

    private MerchantBranding defaultBrandingObject(Merchant merchant) {
        return MerchantBranding.builder()
                .merchant(merchant)
                .primaryColor("#0B7F7B")
                .secondaryColor("#F4B63F")
                .footerText("© " + merchant.getName() + ". All rights reserved.")
                .build();
    }

    private MerchantBrandingResponse toBrandingResponse(Merchant merchant, MerchantBranding branding) {
        return MerchantBrandingResponse.builder()
                .id(branding.getId())
                .merchantId(merchant.getId())
                .logoUrl(branding.getLogoUrl())
                .faviconUrl(branding.getFaviconUrl())
                .primaryColor(branding.getPrimaryColor())
                .secondaryColor(branding.getSecondaryColor())
                .customCss(branding.getCustomCss())
                .footerText(branding.getFooterText())
                .updatedAt(branding.getUpdatedAt())
                .build();
    }

    private static String validateOptionalUrl(String rawUrl, String fieldName) {
        String trimmed = trimToNull(rawUrl);
        if (trimmed == null) return null;
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
            throw new IllegalArgumentException(fieldName + " must start with http:// or https://");
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
