package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.respone.JoinCampaignResponse;
import com.aitravel.reseller.dto.request.CampaignRequest;
import com.aitravel.reseller.dto.respone.CampaignResponse;
import com.aitravel.reseller.entity.Campaign;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.entity.Reseller;
import com.aitravel.reseller.entity.Role;
import com.aitravel.reseller.entity.TrackingLink;
import com.aitravel.reseller.entity.User;
import com.aitravel.reseller.exception.ForbiddenException;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.CampaignRepository;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.ProductRepository;
import com.aitravel.reseller.repository.ResellerRepository;
import com.aitravel.reseller.repository.TrackingLinkRepository;
import com.aitravel.reseller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;
    private final ResellerRepository resellerRepository;
    private final TrackingLinkRepository trackingLinkRepository;
    private final UserRepository userRepository;

    @Transactional
    public JoinCampaignResponse joinCampaign(UUID campaignId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ForbiddenException("Người dùng không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ForbiddenException("Tài khoản người dùng không hoạt động");
        }

        boolean isReseller = user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(role -> role.equals("RESELLER") || role.equals("ROLE_RESELLER"));

        if (!isReseller) {
            throw new ForbiddenException("Người dùng không có quyền tham gia chiến dịch");
        }

        Reseller reseller = resellerRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("Auto-creating Reseller profile for testing for user: {}", user.getEmail());
                    Reseller newReseller = Reseller.builder()
                            .user(user)
                            .referralCode("RES_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                            .status("ACTIVE")
                            .build();
                    return resellerRepository.saveAndFlush(newReseller);
                });

        if (!"ACTIVE".equalsIgnoreCase(reseller.getStatus())) {
            throw new ForbiddenException("Tài khoản Reseller không hoạt động");
        }

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chiến dịch"));

        if (!"ACTIVE".equalsIgnoreCase(campaign.getStatus())) {
            throw new IllegalStateException("Chiến dịch không hoạt động");
        }

        Optional<TrackingLink> existingLinkOpt = trackingLinkRepository.findByCampaignIdAndResellerId(campaignId, reseller.getId());
        if (existingLinkOpt.isPresent()) {
            return buildResponse(existingLinkOpt.get(), "Bạn đã tham gia chiến dịch này");
        }

        try {
            TrackingLink newLink = TrackingLink.builder()
                    .reseller(reseller)
                    .campaign(campaign)
                    .product(campaign.getProduct())
                    .trackingCode(generateTrackingCode(user.getFullName()))
                    .landingSlug(generateLandingSlug(user.getFullName(), campaign.getProduct().getName()))
                    .commissionSnapshot(campaign.getCommissionRate())
                    .resellerSplitSnapshot(campaign.getResellerSplitRate())
                    .build();

            newLink = trackingLinkRepository.saveAndFlush(newLink);
            return buildResponse(newLink, "Tham gia chiến dịch thành công");
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent join request detected for campaign: {} and reseller: {}", campaignId, reseller.getId());
            TrackingLink existingLink = trackingLinkRepository.findByCampaignIdAndResellerId(campaignId, reseller.getId())
                    .orElseThrow(() -> new RuntimeException("Unexpected error during concurrent join"));
            return buildResponse(existingLink, "Bạn đã tham gia chiến dịch này");
        }
    }

    private String generateTrackingCode(String name) {
        String base = (name != null ? removeAccents(name).replaceAll("[^a-zA-Z0-9]", "").toUpperCase() : "RES");
        if (base.isEmpty()) base = "RES";
        if (base.length() > 20) base = base.substring(0, 20);
        String code = base + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        while (trackingLinkRepository.existsByTrackingCode(code)) {
            code = base + "_" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
        }
        return code;
    }

    private String generateLandingSlug(String name, String productName) {
        String baseName = name != null ? removeAccents(name).replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "-").toLowerCase() : "reseller";
        String baseProduct = productName != null ? removeAccents(productName).replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "-").toLowerCase() : "product";
        if (baseName.isEmpty()) baseName = "reseller";
        if (baseProduct.isEmpty()) baseProduct = "product";
        String base = "/r/" + baseName + "-" + baseProduct + "-" + UUID.randomUUID().toString().substring(0, 4);
        if (base.length() > 140) {
            base = base.substring(0, 140) + "-" + UUID.randomUUID().toString().substring(0, 4);
        }

        while (trackingLinkRepository.existsByLandingSlug(base)) {
            base = "/r/" + baseName + "-" + baseProduct + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return base;
    }

    private String removeAccents(String value) {
        String temp = Normalizer.normalize(value, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("Đ", "D").replace("đ", "d");
    }

    private JoinCampaignResponse buildResponse(TrackingLink link, String message) {
        return JoinCampaignResponse.builder()
                .message(message)
                .campaignId(link.getCampaign().getId())
                .productId(link.getProduct().getId())
                .trackingCode(link.getTrackingCode())
                .landingSlug(link.getLandingSlug())
                .commissionSnapshot(link.getCommissionSnapshot())
                .resellerSplitSnapshot(link.getResellerSplitSnapshot())
                .build();
    }

    @Transactional
    public CampaignResponse createCampaign(CampaignRequest request) {
        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Merchant"));
        
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Product"));

        Campaign campaign = Campaign.builder()
                .merchant(merchant)
                .product(product)
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .commissionRate(request.getCommissionRate())
                .resellerSplitRate(request.getResellerSplitRate() != null ? request.getResellerSplitRate() : new java.math.BigDecimal("60.00"))
                .guidelines(request.getGuidelines())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdAt(java.time.Instant.now())
                .build();

        campaign = campaignRepository.save(campaign);
        return mapToCampaignResponse(campaign);
    }

    @Transactional
    public CampaignResponse updateCampaign(UUID id, CampaignRequest request) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chiến dịch"));

        if (!campaign.getMerchant().getId().equals(request.getMerchantId())) {
            Merchant merchant = merchantRepository.findById(request.getMerchantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Merchant"));
            campaign.setMerchant(merchant);
        }

        if (!campaign.getProduct().getId().equals(request.getProductId())) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Product"));
            campaign.setProduct(product);
        }

        campaign.setName(request.getName());
        campaign.setStartDate(request.getStartDate());
        campaign.setEndDate(request.getEndDate());
        campaign.setCommissionRate(request.getCommissionRate());
        
        if (request.getResellerSplitRate() != null) {
            campaign.setResellerSplitRate(request.getResellerSplitRate());
        }
        if (request.getGuidelines() != null) {
            campaign.setGuidelines(request.getGuidelines());
        }
        if (request.getStatus() != null) {
            campaign.setStatus(request.getStatus());
        }

        campaign = campaignRepository.save(campaign);
        return mapToCampaignResponse(campaign);
    }

    public CampaignResponse getCampaignById(UUID id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chiến dịch"));
        return mapToCampaignResponse(campaign);
    }

    public List<CampaignResponse> getAllCampaigns() {
        return campaignRepository.findAll().stream()
                .map(this::mapToCampaignResponse)
                .collect(Collectors.toList());
    }


    private CampaignResponse mapToCampaignResponse(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .merchantId(campaign.getMerchant().getId())
                .merchantName(campaign.getMerchant().getName())
                .productId(campaign.getProduct().getId())
                .productName(campaign.getProduct().getName())
                .name(campaign.getName())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .commissionRate(campaign.getCommissionRate())
                .resellerSplitRate(campaign.getResellerSplitRate())
                .guidelines(campaign.getGuidelines())
                .status(campaign.getStatus())
                .createdAt(campaign.getCreatedAt())
                .build();
    }
}