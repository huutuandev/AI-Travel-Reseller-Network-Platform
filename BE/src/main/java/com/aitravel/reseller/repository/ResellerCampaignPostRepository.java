package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.ResellerCampaignPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResellerCampaignPostRepository extends JpaRepository<ResellerCampaignPost, UUID> {
    List<ResellerCampaignPost> findByCampaignApplicationId(UUID campaignApplicationId);
    List<ResellerCampaignPost> findBySocialAccountId(UUID socialAccountId);
    Optional<ResellerCampaignPost> findBySocialAccountIdAndPlatformAndExternalVideoId(
            UUID socialAccountId, String platform, String externalVideoId);
}
