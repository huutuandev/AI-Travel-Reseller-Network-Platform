package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    long countByMerchantIdAndStatus(UUID merchantId, String status);

    @Query("SELECT SUM(o.amount) FROM com.aitravel.reseller.entity.Order o " +
           "WHERE o.merchant.id = :merchantId AND o.status = :status")
    BigDecimal sumAmountByMerchantIdAndStatus(@Param("merchantId") UUID merchantId,
                                              @Param("status") String status);
}