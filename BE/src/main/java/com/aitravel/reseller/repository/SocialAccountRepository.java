package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
    List<SocialAccount> findByResellerId(UUID resellerId);
    Optional<SocialAccount> findByResellerIdAndPlatformAndUsername(UUID resellerId, String platform, String username);
}
