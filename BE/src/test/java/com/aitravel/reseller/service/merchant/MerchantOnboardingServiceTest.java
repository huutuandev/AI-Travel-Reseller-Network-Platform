package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.MerchantOnboardingCompleteRequest;
import com.aitravel.reseller.dto.request.merchant.MerchantOnboardingRegisterRequest;
import com.aitravel.reseller.dto.request.merchant.MerchantVerifyOtpRequest;
import com.aitravel.reseller.dto.respone.merchant.MerchantOnboardingCompleteResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantOtpVerifyResponse;
import com.aitravel.reseller.dto.respone.merchant.SubdomainCheckResponse;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.MerchantBranding;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.repository.MerchantBrandingRepository;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.RoleRepository;
import com.aitravel.reseller.repository.UserRepository;
import com.aitravel.reseller.security.jwt.JwtUtil;
import com.aitravel.reseller.service.EmailService;
import com.aitravel.reseller.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantOnboardingServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private MerchantBrandingRepository brandingRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RedisService redisService;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private MerchantOnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(onboardingService, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    void registerInitialInfo_ShouldSaveDraftAndSendOtp() {
        MerchantOnboardingRegisterRequest req = MerchantOnboardingRegisterRequest.builder()
                .fullName("Avery Morgan")
                .businessName("Da Nang Travel")
                .email("Avery@DaNangTravel.com")
                .build();

        when(userRepository.existsByEmailIgnoreCase("avery@danangtravel.com")).thenReturn(false);
        when(redisService.exists("merchant:onboarding:resend:avery@danangtravel.com")).thenReturn(false);

        onboardingService.registerInitialInfo(req);

        verify(redisService).save(eq("merchant:onboarding:pending:avery@danangtravel.com"), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq("avery@danangtravel.com"), otpCaptor.capture());
        assertTrue(otpCaptor.getValue().matches("^\\d{6}$"));
    }

    @Test
    void verifyOtp_ShouldIssueOnboardingTokenAndSuggestSubdomain() {
        MerchantVerifyOtpRequest req = MerchantVerifyOtpRequest.builder()
                .email("avery@danangtravel.com")
                .otp("123456")
                .build();

        when(redisService.get("merchant:onboarding:otp:avery@danangtravel.com")).thenReturn("123456");
        when(redisService.get("merchant:onboarding:pending:avery@danangtravel.com"))
                .thenReturn("{\"email\":\"avery@danangtravel.com\",\"fullName\":\"Avery Morgan\",\"businessName\":\"Đà Nẵng Travel\"}");
        when(merchantRepository.existsBySubdomainIgnoreCase("da-nang-travel")).thenReturn(false);

        MerchantOtpVerifyResponse res = onboardingService.verifyOtp(req);

        assertNotNull(res.getOnboardingToken());
        assertEquals("avery@danangtravel.com", res.getEmail());
        assertEquals("Avery Morgan", res.getFullName());
        assertEquals("Đà Nẵng Travel", res.getBusinessName());
        assertEquals("da-nang-travel", res.getSuggestedSubdomain());

        verify(redisService).delete("merchant:onboarding:otp:avery@danangtravel.com");
        verify(redisService).delete("merchant:onboarding:pending:avery@danangtravel.com");
    }

    @Test
    void checkSubdomain_ShouldRejectReservedAndExisting() {
        SubdomainCheckResponse reserved = onboardingService.checkSubdomain("admin");
        assertFalse(reserved.isAvailable());

        when(merchantRepository.existsBySubdomainIgnoreCase("danangtravel")).thenReturn(true);
        SubdomainCheckResponse taken = onboardingService.checkSubdomain("DaNangTravel");
        assertFalse(taken.isAvailable());

        when(merchantRepository.existsBySubdomainIgnoreCase("wayfarer-vn")).thenReturn(false);
        SubdomainCheckResponse available = onboardingService.checkSubdomain("wayfarer-vn");
        assertTrue(available.isAvailable());
    }

    @Test
    void completeOnboarding_ShouldCreateActiveUserMerchantBrandingAndReturnTokens() {
        String token = UUID.randomUUID().toString();
        MerchantOnboardingCompleteRequest req = MerchantOnboardingCompleteRequest.builder()
                .onboardingToken(token)
                .password("SecurePass@123")
                .businessName("Wayfarer Vietnam")
                .subdomain("wayfarer-vn")
                .customDomain("https://wayfarer.com.vn/home")
                .phone("0905123456")
                .address("123 Bach Dang, Da Nang")
                .description("Central Vietnam tours")
                .build();

        when(redisService.get("merchant:onboarding:token:" + token))
                .thenReturn("{\"email\":\"avery@wayfarer.com.vn\",\"fullName\":\"Avery Morgan\",\"businessName\":\"Wayfarer\"}");
        when(userRepository.existsByEmailIgnoreCase("avery@wayfarer.com.vn")).thenReturn(false);
        when(merchantRepository.existsBySubdomainIgnoreCase("wayfarer-vn")).thenReturn(false);
        when(merchantRepository.existsByCustomDomainIgnoreCase("wayfarer.com.vn")).thenReturn(false);
        when(merchantRepository.existsByCodeIgnoreCase("WAYFARER-VN")).thenReturn(false);

        Role merchantRole = Role.builder().id(UUID.randomUUID()).name("ROLE_MERCHANT").scope("GLOBAL").build();
        when(roleRepository.findByName("ROLE_MERCHANT")).thenReturn(Optional.of(merchantRole));
        when(passwordEncoder.encode("SecurePass@123")).thenReturn("encoded_hash");

        UUID userId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        UUID merchantId = UUID.randomUUID();
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(inv -> {
            Merchant m = inv.getArgument(0);
            m.setId(merchantId);
            return m;
        });

        when(brandingRepository.save(any(MerchantBranding.class))).thenAnswer(inv -> {
            MerchantBranding b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        when(jwtUtil.generateAccessToken(any())).thenReturn("jwt_access_token");

        MerchantOnboardingCompleteResponse response = onboardingService.completeOnboarding(req);

        assertNotNull(response);
        assertEquals("jwt_access_token", response.getAuth().getAccessToken());
        assertNotNull(response.getAuth().getRefreshToken());
        assertEquals("ACTIVE", response.getMerchant().getStatus());
        assertEquals("WAYFARER-VN", response.getMerchant().getCode());
        assertEquals("wayfarer-vn", response.getMerchant().getSubdomain());
        assertEquals("wayfarer.com.vn", response.getMerchant().getCustomDomain());
        assertEquals("avery@wayfarer.com.vn", response.getMerchant().getContactEmail());
        assertEquals("#0B7F7B", response.getMerchant().getBranding().getPrimaryColor());

        verify(redisService).delete("merchant:onboarding:token:" + token);
    }
}
