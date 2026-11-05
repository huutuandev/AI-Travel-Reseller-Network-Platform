package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByMerchantId(UUID merchantId);
    List<Booking> findByCustomerId(UUID customerId);
    List<Booking> findByProductId(UUID productId);
    List<Booking> findByTrackingLinkId(UUID trackingLinkId);
}
