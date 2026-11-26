package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.UpdateMerchantBrandingRequest;
import com.aitravel.reseller.dto.request.merchant.UpdateMerchantProfileRequest;
import com.aitravel.reseller.dto.respone.merchant.MerchantBrandingResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantProfileResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.MerchantBranding;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.repository.MerchantBrandingRepository;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantProfileServiceTest {

    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantBrandingRepository brandingRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MerchantProfileService profileService;

    private User merchantUser;
    private Merchant merchant;
    private MerchantBranding branding;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(UUID.randomUUID()).name("ROLE_MERCHANT").scope("GLOBAL").build();
        merchantUser = User.builder()
                .id(UUID.randomUUID())
                .email("merchant@test.com")
                .fullName("Da Nang Travel Admin")
                .phone("0905000002")
                .status("ACTIVE")
                .roles(Set.of(role))
                .build();

        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .user(merchantUser)
                .code("DN-TRAVEL-001")
                .name("Da Nang Travel")
                .description("Central Vietnam Tours")
                .contactEmail("merchant@test.com")
                .phone("0905000002")
                .address("Da Nang, Vietnam")
                .subdomain("danangtravel")
                .customDomain("danangtravel.vn")
                .status("ACTIVE")
                .version(0L)
                .build();

        branding = MerchantBranding.builder()
                .id(UUID.randomUUID())
                .merchant(merchant)
                .primaryColor("#0B7F7B")
                .secondaryColor("#F4B63F")
                .logoUrl("https://cdn.example.com/logo.png")
                .build();

        CustomUserDetails principal = new CustomUserDetails(merchantUser);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfile_ShouldReturnMerchantAndBranding() {
        when(merchantRepository.findByUserId(merchantUser.getId())).thenReturn(Optional.of(merchant));
        when(brandingRepository.findByMerchantId(merchant.getId())).thenReturn(Optional.of(branding));

        MerchantProfileResponse res = profileService.getMyProfile();

        assertEquals(merchant.getId(), res.getId());
        assertEquals("Da Nang Travel", res.getBusinessName());
        assertEquals("danangtravel", res.getSubdomain());
        assertEquals("danangtravel.vn", res.getCustomDomain());
        assertEquals("Da Nang Travel Admin", res.getOwnerFullName());
        assertNotNull(res.getBranding());
        assertEquals("#0B7F7B", res.getBranding().getPrimaryColor());
    }

    @Test
    void updateMyProfile_ShouldUpdateMerchantAndOwner() {
        when(merchantRepository.findByUserId(merchantUser.getId())).thenReturn(Optional.of(merchant));
        when(merchantRepository.findByCustomDomainIgnoreCase("newdomain.vn")).thenReturn(Optional.empty());
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brandingRepository.findByMerchantId(merchant.getId())).thenReturn(Optional.of(branding));

        UpdateMerchantProfileRequest req = UpdateMerchantProfileRequest.builder()
                .businessName("Da Nang Luxury Travel")
                .description("Updated description")
                .contactEmail("support@newdomain.vn")
                .phone("0912345678")
                .address("456 Vo Nguyen Giap, Da Nang")
                .customDomain("https://newdomain.vn/")
                .ownerFullName("New Owner Name")
                .ownerPhone("0988888888")
                .build();

        MerchantProfileResponse res = profileService.updateMyProfile(req);

        assertEquals("Da Nang Luxury Travel", res.getBusinessName());
        assertEquals("newdomain.vn", res.getCustomDomain());
        assertEquals("support@newdomain.vn", res.getContactEmail());
        assertEquals("New Owner Name", res.getOwnerFullName());
        assertEquals("0988888888", res.getOwnerPhone());
    }

    @Test
    void updateMyBranding_ShouldSaveColorsAndLinks() {
        when(merchantRepository.findByUserId(merchantUser.getId())).thenReturn(Optional.of(merchant));
        when(brandingRepository.findByMerchantId(merchant.getId())).thenReturn(Optional.of(branding));
        when(brandingRepository.save(any(MerchantBranding.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMerchantBrandingRequest req = UpdateMerchantBrandingRequest.builder()
                .logoUrl("https://cdn.example.com/new-logo.png")
                .faviconUrl("https://cdn.example.com/favicon.ico")
                .primaryColor("#112233")
                .secondaryColor("#abcdef")
                .footerText("Custom Footer 2026")
                .build();

        MerchantBrandingResponse res = profileService.updateMyBranding(req);

        assertEquals("https://cdn.example.com/new-logo.png", res.getLogoUrl());
        assertEquals("#112233", res.getPrimaryColor());
        assertEquals("#ABCDEF", res.getSecondaryColor());
        assertEquals("Custom Footer 2026", res.getFooterText());
    }

    @Test
    void updateMyProfile_ShouldRejectWhenMerchantIsSuspended() {
        merchant.setStatus("SUSPENDED");
        when(merchantRepository.findByUserId(merchantUser.getId())).thenReturn(Optional.of(merchant));

        UpdateMerchantProfileRequest req = UpdateMerchantProfileRequest.builder()
                .businessName("Blocked Update")
                .contactEmail("merchant@test.com")
                .build();

        assertThrows(AccessDeniedException.class, () -> profileService.updateMyProfile(req));
    }
}
