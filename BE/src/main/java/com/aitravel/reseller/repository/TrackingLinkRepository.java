package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.TrackingLink;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingLinkRepository extends JpaRepository<TrackingLink, UUID> {

    Optional<TrackingLink> findByTrackingCode(String trackingCode);

    @EntityGraph(attributePaths = {
            "campaignApplication",
            "campaignApplication.campaign",
            "campaignApplication.campaign.product",
            "campaignApplication.campaign.product.merchant"
    })
    Optional<TrackingLink> findWithChatScopeByTrackingCode(String trackingCode);
}