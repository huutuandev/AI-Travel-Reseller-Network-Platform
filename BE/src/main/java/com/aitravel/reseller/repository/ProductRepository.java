package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
        SELECT p
        FROM Product p
        WHERE p.merchant.id = :merchantId
        AND (:status = '' OR p.availabilityStatus = :status)
        AND (
            :search = ''
            OR LOWER(p.title) LIKE CONCAT('%', LOWER(:search), '%')
            OR LOWER(COALESCE(p.categoryCode, ''))
                    LIKE CONCAT('%', LOWER(:search), '%')
            OR LOWER(COALESCE(p.destinationCode, ''))
                    LIKE CONCAT('%', LOWER(:search), '%')
        )
        """)
    Page<Product> searchForMerchant(
            @Param("merchantId") UUID merchantId,
            @Param("search") String search,
            @Param("status") String status,
            Pageable pageable
    );

    Optional<Product> findByMerchantIdAndId(UUID merchantId, UUID id);

    boolean existsByMerchantIdAndSlug(UUID merchantId, String slug);

    List<Product> findByMerchantId(UUID merchantId);

    Optional<Product> findByMerchantIdAndSlug(UUID merchantId, String slug);
}