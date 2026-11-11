package com.aitravel.reseller.service.chat;

import com.aitravel.reseller.dto.chat.ChatSessionContext;
import com.aitravel.reseller.entity.KnowledgeDocument;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeIndexingService {

    private static final String ACTIVE = "ACTIVE";

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunker knowledgeChunker;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final KnowledgeVectorRepository knowledgeVectorRepository;

    public void ensureIndexed(ChatSessionContext context) {
        if (context == null
                || context.getMerchantId() == null
                || context.getProductId() == null
                || context.getKnowledgeDocumentIds() == null
                || context.getKnowledgeDocumentIds().isEmpty()) {
            throw new IllegalArgumentException("Valid chat knowledge context is required.");
        }

        List<KnowledgeDocument> activeDocuments =
                knowledgeDocumentRepository.findByMerchantIdAndProductIdAndStatusOrderByIdAsc(
                        context.getMerchantId(),
                        context.getProductId(),
                        ACTIVE
                );

        Map<UUID, KnowledgeDocument> documentsById = new HashMap<>();
        for (KnowledgeDocument document : activeDocuments) {
            if (document.getContent() != null && !document.getContent().isBlank()) {
                documentsById.put(document.getId(), document);
            }
        }

        for (UUID documentId : context.getKnowledgeDocumentIds()) {
            KnowledgeDocument document = documentsById.get(documentId);
            if (document == null) {
                throw new ResourceNotFoundException(
                        "An active knowledge document for this chat session is no longer available.");
            }

            if (knowledgeVectorRepository.hasVectors(documentId)) {
                continue;
            }

            List<String> chunks = knowledgeChunker.split(document.getContent());
            if (chunks.isEmpty()) {
                throw new ResourceNotFoundException(
                        "No indexable content is available for this knowledge document.");
            }

            List<List<Double>> embeddings = new ArrayList<>(chunks.size());
            for (String chunk : chunks) {
                embeddings.add(knowledgeEmbeddingService.embedDocument(chunk));
            }

            knowledgeVectorRepository.replaceChunks(documentId, chunks, embeddings);
        }
    }
}