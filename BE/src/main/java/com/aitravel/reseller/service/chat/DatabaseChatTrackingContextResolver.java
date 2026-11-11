package com.aitravel.reseller.service.chat;

import com.aitravel.reseller.dto.chat.ChatSessionContext;
import com.aitravel.reseller.entity.Campaign;
import com.aitravel.reseller.entity.CampaignApplication;
import com.aitravel.reseller.entity.KnowledgeDocument;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.entity.Product;
import com.aitravel.reseller.entity.TrackingLink;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.KnowledgeDocumentRepository;
import com.aitravel.reseller.repository.TrackingLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatabaseChatTrackingContextResolver implements ChatTrackingContextResolver {

    private static final String ACTIVE = "ACTIVE";

    private final TrackingLinkRepository trackingLinkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Override
    @Transactional(readOnly = true)
    public ChatSessionContext resolve(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking link is required to start a chat session.");
        }

        TrackingLink trackingLink = trackingLinkRepository
                .findWithChatScopeByTrackingCode(trackingCode.trim())
                .orElseThrow(() -> invalidTrackingLink());

        if (!ACTIVE.equals(trackingLink.getStatus())) {
            throw invalidTrackingLink();
        }

        CampaignApplication application = trackingLink.getCampaignApplication();
        if (application == null || !"APPROVED".equals(application.getStatus())) {
            throw invalidTrackingLink();
        }

        Campaign campaign = application.getCampaign();
        if (campaign == null || !ACTIVE.equals(campaign.getStatus()) || !isWithinCampaignPeriod(campaign)) {
            throw invalidTrackingLink();
        }

        Product product = campaign.getProduct();
        if (product == null || !"AVAILABLE".equals(product.getAvailabilityStatus())) {
            throw invalidTrackingLink();
        }

        Merchant merchant = product.getMerchant();
        if (merchant == null || !ACTIVE.equals(merchant.getStatus())) {
            throw invalidTrackingLink();
        }

        UUID merchantId = merchant.getId();
        UUID productId = product.getId();

        if (merchantId == null || productId == null) {
            throw invalidTrackingLink();
        }

        List<UUID> knowledgeDocumentIds = knowledgeDocumentRepository
                .findByMerchantIdAndProductIdAndStatusOrderByIdAsc(merchantId, productId, ACTIVE)
                .stream()
                .filter(document -> document.getContent() != null && !document.getContent().isBlank())
                .map(KnowledgeDocument::getId)
                .toList();

        if (knowledgeDocumentIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No active knowledge document with content is configured for this product.");
        }

        return ChatSessionContext.builder()
                .trackingCode(trackingLink.getTrackingCode())
                .merchantId(merchantId)
                .productId(productId)
                .knowledgeDocumentIds(knowledgeDocumentIds)
                .build();
    }

    private boolean isWithinCampaignPeriod(Campaign campaign) {
        Instant now = Instant.now();

        return (campaign.getStartDate() == null || !campaign.getStartDate().isAfter(now))
                && (campaign.getEndDate() == null || campaign.getEndDate().isAfter(now));
    }

    private ResourceNotFoundException invalidTrackingLink() {
        return new ResourceNotFoundException("Tracking link is invalid or inactive.");
    }
}