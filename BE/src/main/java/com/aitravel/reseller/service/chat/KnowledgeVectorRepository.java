package com.aitravel.reseller.service.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class KnowledgeVectorRepository {

    private static final String TABLE_NAME = "travel_v2.knowledge_vectors";

    private final NamedParameterJdbcTemplate jdbc;

    @Value("${ai.embedding.dimensions:768}")
    private int dimensions;

    public boolean hasVectors(UUID knowledgeDocumentId) {
        Boolean exists = jdbc.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM travel_v2.knowledge_vectors
                    WHERE knowledge_document_id = :documentId
                )
                """,
                new MapSqlParameterSource("documentId", knowledgeDocumentId),
                Boolean.class
        );

        return Boolean.TRUE.equals(exists);
    }

    @Transactional
    public void replaceChunks(
            UUID knowledgeDocumentId,
            List<String> contents,
            List<List<Double>> embeddings
    ) {
        if (knowledgeDocumentId == null) {
            throw new IllegalArgumentException("Knowledge document id is required.");
        }

        if (contents == null || embeddings == null || contents.size() != embeddings.size()) {
            throw new IllegalArgumentException("Each knowledge chunk must have one embedding.");
        }

        if (contents.isEmpty()) {
            throw new IllegalArgumentException("At least one knowledge chunk is required.");
        }

        jdbc.update(
                """
                DELETE FROM travel_v2.knowledge_vectors
                WHERE knowledge_document_id = :documentId
                """,
                new MapSqlParameterSource("documentId", knowledgeDocumentId)
        );

        SqlParameterSource[] batch = IntStream.range(0, contents.size())
                .mapToObj(index -> {
                    String content = contents.get(index);
                    List<Double> embedding = embeddings.get(index);

                    if (content == null || content.isBlank()) {
                        throw new IllegalArgumentException("Knowledge chunk content cannot be blank.");
                    }

                    validateEmbedding(embedding);

                    return new MapSqlParameterSource()
                            .addValue("documentId", knowledgeDocumentId)
                            .addValue("content", content)
                            .addValue("embedding", toVectorLiteral(embedding));
                })
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(
                """
                INSERT INTO travel_v2.knowledge_vectors
                    (knowledge_document_id, content, embedding)
                VALUES
                    (:documentId, :content, CAST(:embedding AS public.vector))
                """,
                batch
        );
    }

    public List<KnowledgeChunkMatch> findNearest(
            List<UUID> knowledgeDocumentIds,
            List<Double> queryEmbedding,
            int limit
    ) {
        if (knowledgeDocumentIds == null || knowledgeDocumentIds.isEmpty()) {
            return List.of();
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Result limit must be greater than zero.");
        }

        validateEmbedding(queryEmbedding);

        String queryVector = toVectorLiteral(queryEmbedding);

        return jdbc.query(
                """
                SELECT
                    id,
                    knowledge_document_id,
                    content,
                    1 - (
                        embedding OPERATOR(public.<=>)
                        CAST(:queryEmbedding AS public.vector)
                    ) AS similarity
                FROM travel_v2.knowledge_vectors
                WHERE knowledge_document_id IN (:documentIds)
                ORDER BY
                    embedding OPERATOR(public.<=>)
                    CAST(:queryEmbedding AS public.vector),
                    id
                LIMIT :limit
                """,
                new MapSqlParameterSource()
                        .addValue("queryEmbedding", queryVector)
                        .addValue("documentIds", knowledgeDocumentIds)
                        .addValue("limit", limit),
                this::mapMatch
        );
    }

    private void validateEmbedding(List<Double> embedding) {
        if (embedding == null || embedding.size() != dimensions) {
            throw new IllegalArgumentException(
                    "Embedding must contain exactly " + dimensions + " dimensions.");
        }

        for (Double value : embedding) {
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Embedding contains an invalid value.");
            }
        }
    }

    private String toVectorLiteral(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private KnowledgeChunkMatch mapMatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new KnowledgeChunkMatch(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("knowledge_document_id", UUID.class),
                resultSet.getString("content"),
                resultSet.getDouble("similarity")
        );
    }

    public record KnowledgeChunkMatch(
            UUID id,
            UUID knowledgeDocumentId,
            String content,
            double similarity
    ) {
    }
}