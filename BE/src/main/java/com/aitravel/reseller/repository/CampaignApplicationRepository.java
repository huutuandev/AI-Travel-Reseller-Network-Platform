package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.CampaignApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, UUID> {
    List<CampaignApplication> findByCampaignId(UUID campaignId);
    List<CampaignApplication> findByResellerId(UUID resellerId);
    Optional<CampaignApplication> findByIdAndResellerId(UUID id, UUID resellerId);
    Optional<CampaignApplication> findByCampaignIdAndResellerId(UUID campaignId, UUID resellerId);
}
