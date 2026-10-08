package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Reseller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResellerRepository extends JpaRepository<Reseller, UUID> {
    Optional<Reseller> findByUserId(UUID userId);
}
