package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.MerchantBranding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantBrandingRepository extends JpaRepository<MerchantBranding, UUID> {
    Optional<MerchantBranding> findByMerchantId(UUID merchantId);
}
