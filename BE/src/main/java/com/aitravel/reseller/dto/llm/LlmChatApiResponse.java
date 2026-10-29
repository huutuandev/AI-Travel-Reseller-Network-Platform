package com.aitravel.reseller.dto.llm;

import lombok.Data;

import java.util.List;

@Data
public class LlmChatApiResponse {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private LlmMessage message;
    }
}