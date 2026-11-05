package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.PayoutItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutItemRepository extends JpaRepository<PayoutItem, UUID> {
    List<PayoutItem> findByPayoutId(UUID payoutId);
    Optional<PayoutItem> findByCommissionId(UUID commissionId);
}
