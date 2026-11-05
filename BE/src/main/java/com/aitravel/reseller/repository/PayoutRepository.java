package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    List<Payout> findByMerchantId(UUID merchantId);
    List<Payout> findByResellerId(UUID resellerId);
}
