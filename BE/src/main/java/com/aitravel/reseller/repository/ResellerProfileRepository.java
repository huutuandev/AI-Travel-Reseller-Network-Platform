package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.ResellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResellerProfileRepository extends JpaRepository<ResellerProfile, UUID> {
    Optional<ResellerProfile> findByUserId(UUID userId);
}
