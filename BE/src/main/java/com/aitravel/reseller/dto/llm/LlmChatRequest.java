package com.aitravel.reseller.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LlmChatRequest {
    private String model;
    private List<LlmMessage> messages;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;
}