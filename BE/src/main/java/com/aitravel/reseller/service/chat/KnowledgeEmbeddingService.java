package com.aitravel.reseller.service.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeEmbeddingService {

    private final RestClient restClient = RestClient.create();

    @Value("${ai.embedding.api-url}")
    private String apiUrl;

    @Value("${ai.embedding.model}")
    private String model;

    @Value("${ai.embedding.dimensions}")
    private int dimensions;

    @Value("${ai.embedding.document-task-type}")
    private String documentTaskType;

    @Value("${ai.embedding.query-task-type}")
    private String queryTaskType;

    @Value("${ai.llm.api-key}")
    private String apiKey;

    public List<Double> embedDocument(String text) {
        return embed(text, documentTaskType);
    }

    public List<Double> embedQuery(String text) {
        return embed(text, queryTaskType);
    }

    private List<Double> embed(String text, String taskType) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to embed is required.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        Map<String, Object> requestBody = Map.of(
                "model", "models/" + model,
                "content", Map.of(
                        "parts", List.of(Map.of("text", text))
                ),
                "taskType", taskType,
                "outputDimensionality", dimensions
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return extractAndNormalize(response);
        } catch (RestClientException e) {
            throw new RestClientException(
                    "Embedding service request or response processing failed.",
                    e
            );
        }
    }

    private List<Double> extractAndNormalize(Map<String, Object> response) {
        Object embeddingObject = response == null ? null : response.get("embedding");

        if (!(embeddingObject instanceof Map<?, ?> embeddingMap)) {
            throw new RestClientException(
                    "Embedding service response does not contain an embedding object.");
        }

        Object valuesObject = embeddingMap.get("values");
        if (!(valuesObject instanceof List<?> values) || values.size() != dimensions) {
            throw new RestClientException(
                    "Embedding service returned a vector with an unexpected dimension.");
        }

        List<Double> vector = new ArrayList<>(dimensions);
        double squaredNorm = 0.0;

        for (Object item : values) {
            if (!(item instanceof Number number)) {
                throw new RestClientException(
                        "Embedding service returned a non-numeric vector value.");
            }

            double value = number.doubleValue();
            if (!Double.isFinite(value)) {
                throw new RestClientException(
                        "Embedding service returned an invalid vector value.");
            }

            vector.add(value);
            squaredNorm += value * value;
        }

        double norm = Math.sqrt(squaredNorm);
        if (!Double.isFinite(norm) || norm == 0.0) {
            throw new RestClientException(
                    "Embedding service returned a zero-length vector.");
        }

        for (int index = 0; index < vector.size(); index++) {
            vector.set(index, vector.get(index) / norm);
        }

        return List.copyOf(vector);
    }
}