package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByUser_Id(UUID userId);
    boolean existsByUser_Id(UUID userId);
}
