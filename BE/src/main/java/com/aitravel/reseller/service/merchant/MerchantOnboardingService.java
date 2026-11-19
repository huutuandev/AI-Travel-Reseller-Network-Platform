package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.request.merchant.MerchantOnboardingCompleteRequest;
import com.aitravel.reseller.dto.request.merchant.MerchantOnboardingRegisterRequest;
import com.aitravel.reseller.dto.request.merchant.MerchantVerifyOtpRequest;
import com.aitravel.reseller.dto.respone.AuthResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantBrandingResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantOnboardingCompleteResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantOtpVerifyResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantProfileResponse;
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
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.EmailService;
import com.aitravel.reseller.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MerchantOnboardingService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantBrandingRepository brandingRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpirationMs;

    private static final String PENDING_PREFIX = "merchant:onboarding:pending:";
    private static final String OTP_PREFIX = "merchant:onboarding:otp:";
    private static final String OTP_RESEND_PREFIX = "merchant:onboarding:resend:";
    private static final String TOKEN_PREFIX = "merchant:onboarding:token:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private static final long OTP_TTL_MINUTES = 10;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final long ONBOARDING_TOKEN_TTL_MINUTES = 30;

    private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])$");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}$");
    private static final Set<String> RESERVED_SUBDOMAINS = Set.of(
            "www", "api", "admin", "app", "auth", "mail", "smtp", "reseller", "merchant", "platform", "dashboard", "support"
    );

    private static final SecureRandom RANDOM = new SecureRandom();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OnboardingDraft {
        private String email;
        private String fullName;
        private String businessName;
    }

    public void registerInitialInfo(MerchantOnboardingRegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String fullName = request.getFullName().trim();
        String businessName = request.getBusinessName().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email is already registered on the platform");
        }

        if (redisService.exists(OTP_RESEND_PREFIX + email)) {
            throw new IllegalStateException("Please wait before requesting a new OTP");
        }

        OnboardingDraft draft = new OnboardingDraft(email, fullName, businessName);
        String draftJson = writeDraft(draft);

        redisService.save(PENDING_PREFIX + email, draftJson, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        generateAndSendOtp(email);
    }

    public void resendOtp(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("Email is already registered on the platform");
        }
        String draftJson = redisService.get(PENDING_PREFIX + email);
        if (draftJson == null) {
            throw new IllegalArgumentException("Registration session expired or not found. Please fill out the registration form again.");
        }
        if (redisService.exists(OTP_RESEND_PREFIX + email)) {
            throw new IllegalStateException("Please wait before requesting a new OTP");
        }
        // Extend pending draft TTL
        redisService.save(PENDING_PREFIX + email, draftJson, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        generateAndSendOtp(email);
    }

    public MerchantOtpVerifyResponse verifyOtp(MerchantVerifyOtpRequest request) {
        String email = normalizeEmail(request.getEmail());
        String otpKey = OTP_PREFIX + email;
        String savedOtp = redisService.get(otpKey);

        if (savedOtp == null) {
            throw new IllegalArgumentException("OTP expired or not found");
        }
        if (!savedOtp.equals(request.getOtp().trim())) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        String draftJson = redisService.get(PENDING_PREFIX + email);
        if (draftJson == null) {
            throw new IllegalArgumentException("Registration session expired. Please register again.");
        }

        OnboardingDraft draft = readDraft(draftJson);

        String onboardingToken = UUID.randomUUID().toString();
        redisService.save(TOKEN_PREFIX + onboardingToken, draftJson, ONBOARDING_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);

        // Clean up OTP keys
        redisService.delete(otpKey);
        redisService.delete(OTP_RESEND_PREFIX + email);
        redisService.delete(PENDING_PREFIX + email);

        String suggestedSubdomain = suggestAvailableSubdomain(draft.getBusinessName());

        return MerchantOtpVerifyResponse.builder()
                .onboardingToken(onboardingToken)
                .email(draft.getEmail())
                .fullName(draft.getFullName())
                .businessName(draft.getBusinessName())
                .suggestedSubdomain(suggestedSubdomain)
                .build();
    }

    @Transactional(readOnly = true)
    public SubdomainCheckResponse checkSubdomain(String rawSubdomain) {
        if (rawSubdomain == null || rawSubdomain.isBlank()) {
            throw new IllegalArgumentException("Subdomain is required");
        }
        String normalized = rawSubdomain.trim().toLowerCase(Locale.ROOT);
        if (!SUBDOMAIN_PATTERN.matcher(normalized).matches()) {
            return SubdomainCheckResponse.builder()
                    .subdomain(normalized)
                    .available(false)
                    .message("Subdomain must be 3-63 characters, lowercase letters, digits, or hyphens, and cannot start or end with a hyphen")
                    .build();
        }
        if (RESERVED_SUBDOMAINS.contains(normalized)) {
            return SubdomainCheckResponse.builder()
                    .subdomain(normalized)
                    .available(false)
                    .message("This subdomain is reserved by the platform")
                    .build();
        }
        boolean exists = merchantRepository.existsBySubdomainIgnoreCase(normalized);
        return SubdomainCheckResponse.builder()
                .subdomain(normalized)
                .available(!exists)
                .message(exists ? "Subdomain is already taken" : "Subdomain is available")
                .build();
    }

    @Transactional
    public MerchantOnboardingCompleteResponse completeOnboarding(MerchantOnboardingCompleteRequest request) {
        String tokenKey = TOKEN_PREFIX + request.getOnboardingToken().trim();
        String draftJson = redisService.get(tokenKey);
        if (draftJson == null) {
            throw new IllegalArgumentException("Onboarding token is invalid or expired. Please verify your email again.");
        }

        OnboardingDraft draft = readDraft(draftJson);
        String email = normalizeEmail(draft.getEmail());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            redisService.delete(tokenKey);
            throw new IllegalStateException("Email is already registered on the platform");
        }

        String businessName = (request.getBusinessName() != null && !request.getBusinessName().isBlank())
                ? request.getBusinessName().trim()
                : draft.getBusinessName();

        String subdomain = validateAndNormalizeSubdomain(request.getSubdomain());
        if (merchantRepository.existsBySubdomainIgnoreCase(subdomain)) {
            throw new IllegalStateException("Subdomain '" + subdomain + "' is already in use");
        }

        String customDomain = normalizeAndValidateCustomDomain(request.getCustomDomain());
        if (customDomain != null && merchantRepository.existsByCustomDomainIgnoreCase(customDomain)) {
            throw new IllegalStateException("Official website domain '" + customDomain + "' is already linked to another merchant");
        }

        String code = resolveUniqueMerchantCode(request.getCode(), subdomain, businessName);

        String contactEmail = (request.getContactEmail() != null && !request.getContactEmail().isBlank())
                ? normalizeEmail(request.getContactEmail())
                : email;

        String phone = trimToNull(request.getPhone());
        String address = trimToNull(request.getAddress());
        String description = trimToNull(request.getDescription());

        Role merchantRole = roleRepository.findByName("ROLE_MERCHANT")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_MERCHANT")
                        .scope("GLOBAL")
                        .description("Quản lý sản phẩm và chiến dịch")
                        .build()));

        Instant now = Instant.now();
        User owner = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(draft.getFullName())
                .phone(phone)
                .status("ACTIVE")
                .emailVerifiedAt(now)
                .roles(Set.of(merchantRole))
                .build();
        owner = userRepository.save(owner);

        Merchant merchant = Merchant.builder()
                .user(owner)
                .code(code)
                .name(businessName)
                .description(description)
                .contactEmail(contactEmail)
                .phone(phone)
                .address(address)
                .subdomain(subdomain)
                .customDomain(customDomain)
                .status("ACTIVE")
                .build();
        merchant = merchantRepository.save(merchant);

        MerchantBranding branding = MerchantBranding.builder()
                .merchant(merchant)
                .primaryColor("#0B7F7B")
                .secondaryColor("#F4B63F")
                .footerText("© " + businessName + ". All rights reserved.")
                .build();
        branding = brandingRepository.save(branding);

        // Consume onboarding token only after successful persistence
        redisService.delete(tokenKey);

        CustomUserDetails userDetails = new CustomUserDetails(owner);
        AuthResponse authResponse = generateTokens(userDetails);

        MerchantProfileResponse profileResponse = toProfileResponse(merchant, owner, branding);

        return MerchantOnboardingCompleteResponse.builder()
                .auth(authResponse)
                .merchant(profileResponse)
                .build();
    }

    public static String validateAndNormalizeSubdomain(String rawSubdomain) {
        if (rawSubdomain == null || rawSubdomain.isBlank()) {
            throw new IllegalArgumentException("Subdomain is required");
        }
        String normalized = rawSubdomain.trim().toLowerCase(Locale.ROOT);
        if (!SUBDOMAIN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Subdomain must be 3-63 characters, contain only lowercase letters, numbers, and hyphens, and cannot start or end with a hyphen");
        }
        if (RESERVED_SUBDOMAINS.contains(normalized)) {
            throw new IllegalArgumentException("Subdomain '" + normalized + "' is reserved by the platform");
        }
        return normalized;
    }

    public static String normalizeAndValidateCustomDomain(String rawDomain) {
        if (rawDomain == null || rawDomain.isBlank()) {
            return null;
        }
        String cleaned = rawDomain.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("https://")) {
            cleaned = cleaned.substring("https://".length());
        } else if (cleaned.startsWith("http://")) {
            cleaned = cleaned.substring("http://".length());
        }
        int slashIdx = cleaned.indexOf('/');
        if (slashIdx >= 0) {
            cleaned = cleaned.substring(0, slashIdx);
        }
        int portIdx = cleaned.indexOf(':');
        if (portIdx >= 0) {
            cleaned = cleaned.substring(0, portIdx);
        }
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        if (!DOMAIN_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid official website domain format (e.g., example.com or travel.example.vn)");
        }
        return cleaned;
    }

    private String resolveUniqueMerchantCode(String requestedCode, String subdomain, String businessName) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            String normalizedCode = slugify(requestedCode).toUpperCase(Locale.ROOT);
            if (normalizedCode.length() < 3 || normalizedCode.length() > 100) {
                throw new IllegalArgumentException("Business code must be between 3 and 100 alphanumeric characters");
            }
            if (merchantRepository.existsByCodeIgnoreCase(normalizedCode)) {
                throw new IllegalStateException("Business code '" + normalizedCode + "' is already in use");
            }
            return normalizedCode;
        }

        String base = slugify(subdomain.isBlank() ? businessName : subdomain)
                .toUpperCase(Locale.ROOT);
        if (base.isBlank()) {
            base = "MERCHANT";
        }
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        if (!merchantRepository.existsByCodeIgnoreCase(base)) {
            return base;
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            String suffix = String.format("%04d", RANDOM.nextInt(10000));
            String candidate = base + "-" + suffix;
            if (!merchantRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String suggestAvailableSubdomain(String businessName) {
        String base = slugify(businessName).toLowerCase(Locale.ROOT);
        if (base.length() < 3) {
            base = (base.isEmpty() ? "travel" : base + "-travel");
        }
        if (base.length() > 50) {
            base = base.substring(0, 50).replaceAll("-+$", "");
        }
        if (!RESERVED_SUBDOMAINS.contains(base) && !merchantRepository.existsBySubdomainIgnoreCase(base)) {
            return base;
        }
        for (int i = 1; i <= 50; i++) {
            String candidate = base + "-" + i;
            if (!merchantRepository.existsBySubdomainIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return base + "-" + String.format("%04d", RANDOM.nextInt(10000));
    }

    private static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private AuthResponse generateTokens(CustomUserDetails userDetails) {
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String tokenId = UUID.randomUUID().toString();
        String refreshToken = userDetails.getId() + ":" + tokenId;

        String redisKey = REFRESH_TOKEN_PREFIX + userDetails.getId() + ":" + tokenId;
        redisService.save(redisKey, userDetails.getEmail(), refreshTokenExpirationMs, TimeUnit.MILLISECONDS);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }

    private void generateAndSendOtp(String email) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisService.save(OTP_PREFIX + email, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        redisService.save(OTP_RESEND_PREFIX + email, "1", RESEND_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        emailService.sendOtpEmail(email, otp);
    }

    private String writeDraft(OnboardingDraft draft) {
        try {
            return objectMapper.writeValueAsString(draft);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize onboarding session", e);
        }
    }

    private OnboardingDraft readDraft(String json) {
        try {
            return objectMapper.readValue(json, OnboardingDraft.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read onboarding session", e);
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static MerchantProfileResponse toProfileResponse(Merchant merchant, User owner, MerchantBranding branding) {
        MerchantBrandingResponse brandingDto = branding == null ? null : MerchantBrandingResponse.builder()
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

        return MerchantProfileResponse.builder()
                .id(merchant.getId())
                .code(merchant.getCode())
                .businessName(merchant.getName())
                .description(merchant.getDescription())
                .contactEmail(merchant.getContactEmail())
                .phone(merchant.getPhone())
                .address(merchant.getAddress())
                .subdomain(merchant.getSubdomain())
                .customDomain(merchant.getCustomDomain())
                .status(merchant.getStatus())
                .version(merchant.getVersion())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerFullName(owner != null ? owner.getFullName() : null)
                .ownerPhone(owner != null ? owner.getPhone() : null)
                .branding(brandingDto)
                .build();
    }
}
