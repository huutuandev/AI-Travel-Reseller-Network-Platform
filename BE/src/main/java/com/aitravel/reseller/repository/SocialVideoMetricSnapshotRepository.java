package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.SocialVideoMetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SocialVideoMetricSnapshotRepository extends JpaRepository<SocialVideoMetricSnapshot, UUID> {
    List<SocialVideoMetricSnapshot> findByPost_IdOrderByFetchedAtDesc(UUID postId);
}
