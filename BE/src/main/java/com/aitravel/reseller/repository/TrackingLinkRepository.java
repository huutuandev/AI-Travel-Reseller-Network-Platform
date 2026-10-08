package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.TrackingLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrackingLinkRepository extends JpaRepository<TrackingLink, UUID> {
    Optional<TrackingLink> findByCampaignIdAndResellerId(UUID campaignId, UUID resellerId);
    boolean existsByTrackingCode(String trackingCode);
    boolean existsByLandingSlug(String landingSlug);
}
