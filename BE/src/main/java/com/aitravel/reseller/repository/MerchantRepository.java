package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
    Optional<Merchant> findByCode(String code);

    Optional<Merchant> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    Optional<Merchant> findBySubdomain(String subdomain);

    Optional<Merchant> findBySubdomainIgnoreCase(String subdomain);

    boolean existsBySubdomainIgnoreCase(String subdomain);

    Optional<Merchant> findByCustomDomainIgnoreCase(String customDomain);

    boolean existsByCustomDomainIgnoreCase(String customDomain);

    Optional<Merchant> findByUserId(UUID userId);
}