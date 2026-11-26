package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.AiContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiContentRepository extends JpaRepository<AiContent, UUID> {
    List<AiContent> findByCampaignId(UUID campaignId);
    List<AiContent> findByGenerationStatus(String generationStatus);
}
