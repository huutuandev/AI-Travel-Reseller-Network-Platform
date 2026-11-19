package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends
        JpaRepository<Campaign, UUID>,
        JpaSpecificationExecutor<Campaign> {

    List<Campaign> findByProductId(UUID productId);

    List<Campaign> findByStatus(String status);

    Optional<Campaign> findByIdAndProduct_Merchant_Id(
            UUID campaignId,
            UUID merchantId
    );
}