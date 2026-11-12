package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    List<Voucher> findByMerchantId(UUID merchantId);
    Optional<Voucher> findByMerchantIdAndCode(UUID merchantId, String code);
}
