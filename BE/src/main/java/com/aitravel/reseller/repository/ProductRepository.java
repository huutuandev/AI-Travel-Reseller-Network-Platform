package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByMerchantId(UUID merchantId);
    Optional<Product> findByMerchantIdAndSlug(UUID merchantId, String slug);
}
