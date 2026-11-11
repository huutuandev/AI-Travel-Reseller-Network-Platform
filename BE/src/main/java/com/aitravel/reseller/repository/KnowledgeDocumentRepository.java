package com.aitravel.reseller.repository;

import com.aitravel.reseller.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    List<KnowledgeDocument> findByMerchantId(UUID merchantId);

    List<KnowledgeDocument> findByProductId(UUID productId);

    List<KnowledgeDocument> findByStatus(String status);

    List<KnowledgeDocument> findByMerchantIdAndProductIdAndStatusOrderByIdAsc(
            UUID merchantId,
            UUID productId,
            String status
    );
}