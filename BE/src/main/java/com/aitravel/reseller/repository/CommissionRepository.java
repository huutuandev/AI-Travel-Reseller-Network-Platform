package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.Commission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, UUID> {
    Optional<Commission> findByBookingId(UUID bookingId);
    List<Commission> findByResellerId(UUID resellerId);
    List<Commission> findByResellerIdAndStatus(UUID resellerId, String status);
}
