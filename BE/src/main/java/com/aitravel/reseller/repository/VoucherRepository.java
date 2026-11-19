package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository
        extends JpaRepository<Voucher, UUID>,
                JpaSpecificationExecutor<Voucher> {

    Optional<Voucher> findByMerchant_IdAndId(UUID merchantId, UUID id);

    boolean existsByMerchant_IdAndCodeIgnoreCase(UUID merchantId, String code);
    
    boolean existsByMerchant_IdAndCodeIgnoreCaseAndIdNot(
            UUID merchantId,
            String code,
            UUID id
    );

    default Page<Voucher> findMerchantVouchers(
            Specification<Voucher> specification,
            Pageable pageable
    ) {
        return findAll(specification, pageable);
    }

    @Modifying
    @Query("""
            UPDATE Voucher v
            SET v.status = 'EXPIRED'
            WHERE v.merchant.id = :merchantId
              AND v.status <> 'EXPIRED'
              AND v.expiresAt IS NOT NULL
              AND v.expiresAt <= CURRENT_TIMESTAMP
            """)
    int expirePastDueForMerchant(@Param("merchantId") UUID merchantId);
}