package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.SocialAccountOAuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface SocialAccountOAuthCredentialRepository
        extends JpaRepository<SocialAccountOAuthCredential, UUID> {
    Optional<SocialAccountOAuthCredential> findByProviderAndProviderUserId(String provider, String providerUserId);

    @Query("select credential from SocialAccountOAuthCredential credential " +
            "where credential.socialAccount.id = :socialAccountId " +
            "and credential.socialAccount.reseller.id = :resellerId " +
            "and credential.provider = :provider")
    Optional<SocialAccountOAuthCredential> findOwnedCredential(
            @Param("socialAccountId") UUID socialAccountId,
            @Param("resellerId") UUID resellerId,
            @Param("provider") String provider);

    @Query("select credential from SocialAccountOAuthCredential credential " +
            "join fetch credential.socialAccount account " +
            "where account.reseller.id = :resellerId " +
            "and account.status = 'ACTIVE' and credential.provider = :provider")
    List<SocialAccountOAuthCredential> findActiveOwnedCredentials(
            @Param("resellerId") UUID resellerId,
            @Param("provider") String provider);
}
