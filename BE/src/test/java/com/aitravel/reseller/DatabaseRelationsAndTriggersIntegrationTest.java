package com.aitravel.reseller;

import com.aitravel.reseller.entity.*;
import com.aitravel.reseller.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseRelationsAndTriggersIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private MerchantRepository merchantRepository;
    @Autowired private MerchantBrandingRepository brandingRepository;
    @Autowired private ResellerProfileRepository resellerProfileRepository;
    @Autowired private SocialAccountRepository socialAccountRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private CampaignApplicationRepository applicationRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private TrackingLinkRepository trackingLinkRepository;
    @Autowired private ResellerCampaignPostRepository postRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private CommissionRepository commissionRepository;
    @Autowired private PayoutRepository payoutRepository;
    @Autowired private PayoutItemRepository payoutItemRepository;
    @Autowired private AiContentRepository aiContentRepository;
    @Autowired private KnowledgeDocumentRepository knowledgeRepository;

    @Test
    @Transactional
    @DisplayName("Test 1: users - roles relation")
    void testUsersAndRoles() {
        Role role = roleRepository.findByName("ROLE_ADMIN").orElseGet(() ->
                roleRepository.save(Role.builder().name("ROLE_ADMIN").scope("PLATFORM").build()));

        User user = userRepository.save(User.builder()
                .email("test_user_" + UUID.randomUUID() + "@example.com")
                .passwordHash("hashed_pwd")
                .fullName("Test User")
                .status("ACTIVE")
                .roles(Set.of(role))
                .build());

        assertNotNull(user.getId());
        User loaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals(1, loaded.getRoles().size());
        assertEquals("ROLE_ADMIN", loaded.getRoles().iterator().next().getName());
    }

    @Test
    @Transactional
    @DisplayName("Test 2: merchants - merchant_branding relation")
    void testMerchantsAndBranding() {
        User owner = userRepository.save(User.builder()
                .email("merchant_owner_" + UUID.randomUUID() + "@example.com")
                .passwordHash("pwd")
                .fullName("Owner")
                .status("ACTIVE")
                .build());

        String code = "MC_" + UUID.randomUUID().toString().substring(0, 8);
        Merchant merchant = merchantRepository.save(Merchant.builder()
                .user(owner)
                .code(code)
                .name("Demo Merchant")
                .contactEmail("contact@" + code + ".com")
                .subdomain("sub-" + code.toLowerCase())
                .status("ACTIVE")
                .build());

        MerchantBranding branding = brandingRepository.save(MerchantBranding.builder()
                .merchant(merchant)
                .primaryColor("#0B7F7B")
                .secondaryColor("#F4B63F")
                .footerText("Footer Note")
                .build());

        assertNotNull(branding.getId());
        MerchantBranding loaded = brandingRepository.findByMerchantId(merchant.getId()).orElseThrow();
        assertEquals("#0B7F7B", loaded.getPrimaryColor());
    }

    @Test
    @Transactional
    @DisplayName("Test 3: users - reseller_profiles - social_accounts relation")
    void testResellerAndSocialAccounts() {
        User user = userRepository.save(User.builder()
                .email("reseller_" + UUID.randomUUID() + "@example.com")
                .passwordHash("pwd")
                .fullName("Reseller One")
                .status("ACTIVE")
                .build());

        ResellerProfile profile = resellerProfileRepository.save(ResellerProfile.builder()
                .user(user)
                .status("ACTIVE")
                .build());

        SocialAccount account = socialAccountRepository.save(SocialAccount.builder()
                .reseller(profile)
                .platform("TIKTOK")
                .username("travel_tiktok_" + UUID.randomUUID().toString().substring(0, 6))
                .followersCount(50000L)
                .averageViews(25000L)
                .metricsSource("MANUAL")
                .status("ACTIVE")
                .build());

        assertNotNull(account.getId());
        assertEquals(50000L, account.getFollowersCount());
    }

    @Test
    @Transactional
    @DisplayName("Test 4: merchants - products - campaigns - applications - tracking relation with triggers")
    void testFullAffiliateChainAndTriggers() {
        // Merchant & User
        User owner = userRepository.save(User.builder()
                .email("m_owner_" + UUID.randomUUID() + "@example.com")
                .passwordHash("pwd")
                .fullName("Owner 2")
                .status("ACTIVE")
                .build());

        String code = "MC_" + UUID.randomUUID().toString().substring(0, 8);
        Merchant merchant = merchantRepository.save(Merchant.builder()
                .user(owner)
                .code(code)
                .name("Tour Merchant")
                .contactEmail("contact@tour.com")
                .subdomain("sub-" + code.toLowerCase())
                .status("ACTIVE")
                .build());

        // Product
        Product product = productRepository.save(Product.builder()
                .merchant(merchant)
                .title("Ba Na Hills Day Tour")
                .slug("ba-na-day-tour-" + UUID.randomUUID().toString().substring(0, 6))
                .price(new BigDecimal("1200000.00"))
                .availabilityStatus("AVAILABLE")
                .officialCheckoutUrl("https://example.com/checkout/bana")
                .build());

        // Campaign
        Campaign campaign = campaignRepository.save(Campaign.builder()
                .product(product)
                .title("Summer Campaign")
                .commissionType("PERCENTAGE")
                .commissionRate(new BigDecimal("12.50"))
                .status("ACTIVE")
                .build());

        // Reseller
        User rUser = userRepository.save(User.builder()
                .email("r_user_" + UUID.randomUUID() + "@example.com")
                .passwordHash("pwd")
                .fullName("Reseller Aff")
                .status("ACTIVE")
                .build());
        ResellerProfile reseller = resellerProfileRepository.save(ResellerProfile.builder()
                .user(rUser)
                .status("ACTIVE")
                .build());

        // Campaign Application
        CampaignApplication application = applicationRepository.save(CampaignApplication.builder()
                .campaign(campaign)
                .reseller(reseller)
                .commissionTypeSnapshot("PERCENTAGE")
                .commissionRateSnapshot(new BigDecimal("12.50"))
                .status("APPROVED")
                .build());

        assertNotNull(application.getId());

        // Tracking Link
        TrackingLink trackingLink = trackingLinkRepository.save(TrackingLink.builder()
                .campaignApplication(application)
                .reseller(reseller)
                .trackingCode("TRACK_" + UUID.randomUUID().toString().substring(0, 8))
                .targetUrl("https://example.com/tour")
                .status("ACTIVE")
                .clickCount(0)
                .build());

        assertNotNull(trackingLink.getId());

        // Social Account & Campaign Post
        SocialAccount socialAccount = socialAccountRepository.save(SocialAccount.builder()
                .reseller(reseller)
                .platform("YOUTUBE")
                .username("yt_travel_" + UUID.randomUUID().toString().substring(0, 6))
                .followersCount(10000L)
                .averageViews(5000L)
                .metricsSource("MANUAL")
                .status("ACTIVE")
                .build());

        ResellerCampaignPost post = postRepository.save(ResellerCampaignPost.builder()
                .campaignApplication(application)
                .socialAccount(socialAccount)
                .postUrl("https://youtube.com/watch?v=123456")
                .caption("Check out Ba Na Hills!")
                .viewsCount(1500L)
                .likesCount(200L)
                .status("PUBLISHED")
                .build());

        assertNotNull(post.getId());

        // Booking
        Booking booking = bookingRepository.save(Booking.builder()
                .merchant(merchant)
                .product(product)
                .trackingLink(trackingLink)
                .totalAmount(new BigDecimal("1200000.00"))
                .currency("VND")
                .bookingStatus("CONFIRMED")
                .build());

        assertNotNull(booking.getId());

        // Commission
        Commission commission = commissionRepository.save(Commission.builder()
                .booking(booking)
                .reseller(reseller)
                .amount(new BigDecimal("150000.00"))
                .currency("VND")
                .status("PAYABLE")
                .build());

        assertNotNull(commission.getId());

        // Voucher
        Voucher voucher = voucherRepository.save(Voucher.builder()
                .merchant(merchant)
                .campaignApplication(application)
                .code("VOUCHER_" + UUID.randomUUID().toString().substring(0, 6))
                .name("Summer 50k")
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("50000.00"))
                .minOrderAmount(new BigDecimal("500000.00"))
                .usageLimit(100L)
                .usageCount(0L)
                .status("ACTIVE")
                .build());

        assertNotNull(voucher.getId());

        // AI Content & Knowledge Document
        AiContent aiContent = aiContentRepository.save(AiContent.builder()
                .campaign(campaign)
                .title("Ba Na Hills Promo Video Script")
                .contentType("VIDEO_SCRIPT")
                .promptUsed("Write a vibrant 30s TikTok script for Ba Na Hills")
                .generatedContent("{\"title\":\"Ba Na Magic\",\"hook\":\"Have you seen the Golden Bridge?\"}")
                .generationStatus("COMPLETED")
                .build());

        assertNotNull(aiContent.getId());

        KnowledgeDocument doc = knowledgeRepository.save(KnowledgeDocument.builder()
                .merchant(merchant)
                .product(product)
                .title("Ba Na Hills FAQ & Opening Hours")
                .content("Open daily from 7:00 AM to 10:00 PM. Cable car operates continuously.")
                .status("ACTIVE")
                .build());

        assertNotNull(doc.getId());
    }

    @Test
    @Transactional
    @DisplayName("Test 5: Payouts and Payout Items with auto sum trigger")
    void testPayoutAndPayoutItems() {
        User mUser = userRepository.save(User.builder()
                .email("payout_m_" + UUID.randomUUID() + "@example.com")
                .passwordHash("pwd").fullName("Payout Merchant").status("ACTIVE").build());
        String code = "MC_" + UUID.randomUUID().toString().substring(0, 8);
        Merchant merchant = merchantRepository.save(Merchant.builder()
                .user(mUser).code(code).name("Pay Merchant").contactEmail("m@pay.com")
                .subdomain("sub-" + code.toLowerCase()).status("ACTIVE").build());

        User rUser = userRepository.save(User.builder()
                .email("payout_r_" + UUID.randomUUID() + "@example.com")
                .passwordHash("pwd").fullName("Payout Reseller").status("ACTIVE").build());
        ResellerProfile reseller = resellerProfileRepository.save(ResellerProfile.builder()
                .user(rUser).status("ACTIVE").build());

        Product product = productRepository.save(Product.builder()
                .merchant(merchant).title("Day Trip").slug("day-trip-" + UUID.randomUUID().toString().substring(0, 6))
                .price(new BigDecimal("500000.00")).availabilityStatus("AVAILABLE")
                .officialCheckoutUrl("https://pay.com").build());

        Booking booking = bookingRepository.save(Booking.builder()
                .merchant(merchant).product(product).totalAmount(new BigDecimal("500000.00"))
                .currency("VND").bookingStatus("CONFIRMED").build());

        Commission comm = commissionRepository.save(Commission.builder()
                .booking(booking).reseller(reseller).amount(new BigDecimal("75000.00"))
                .currency("VND").status("PAYABLE").build());

        Payout payout = payoutRepository.save(Payout.builder()
                .merchant(merchant).reseller(reseller).payoutStatus("PROCESSING")
                .currency("VND").totalAmount(BigDecimal.ZERO).build());

        PayoutItem item = payoutItemRepository.save(PayoutItem.builder()
                .payout(payout).commission(comm).amount(new BigDecimal("75000.00")).build());

        assertNotNull(item.getId());
    }
}
