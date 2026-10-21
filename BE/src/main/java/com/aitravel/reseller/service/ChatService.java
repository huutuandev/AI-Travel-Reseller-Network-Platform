package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.llm.LlmChatApiResponse;
import com.aitravel.reseller.dto.llm.LlmChatRequest;
import com.aitravel.reseller.dto.llm.LlmMessage;
import com.aitravel.reseller.dto.request.ChatRequest;
import com.aitravel.reseller.dto.respone.ChatResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChatService {

    private static final String HISTORY_PREFIX = "chat:session:";
    private static final long HISTORY_TTL_MINUTES = 30;
    private static final int MAX_HISTORY_MESSAGES = 20;

    private static final String SYSTEM_PROMPT = """
            Bạn là AI Sales Agent của nền tảng du lịch.
            Nhiệm vụ: tư vấn sản phẩm du lịch (vé tham quan, buffet, tour ngày, spa, xe đưa đón)
            bằng tiếng Việt, ngắn gọn, thân thiện.

            Quy tắc bắt buộc:
            - Chỉ dùng thông tin đã được cung cấp. Không bịa giá, tồn kho, khuyến mãi, giờ mở cửa, hoàn tiền.
            - Nếu thiếu thông tin, nói rõ và gợi ý khách liên hệ merchant chính thức.
            - Không nhận thanh toán. Không bảo khách chuyển khoản cho bạn.
            - Hướng khách tới link checkout chính thức khi khách có ý định đặt.
            - Hỏi tối đa 1–2 câu cần thiết (ngày đi, số người lớn/trẻ em) rồi đề xuất.
            - Không nhận mình là nhân viên của merchant.
            """;

    private final RedisService redisService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Value("${ai.llm.api-url}")
    private String llmApiUrl;

    @Value("${ai.llm.api-key}")
    private String llmApiKey;

    @Value("${ai.llm.model}")
    private String llmModel;

    @Value("${ai.llm.temperature}")
    private double llmTemperature;

    @Value("${ai.llm.max-tokens}")
    private int llmMaxTokens;

    public ChatService(RedisService redisService) {
        this.redisService = redisService;
    }

    public ChatResponse chat(ChatRequest request) {
        if (llmApiKey == null || llmApiKey.isBlank()) {
            throw new IllegalStateException("LLM API key is not configured");
        }

        String sessionId = (request.getSessionId() == null || request.getSessionId().isBlank())
                ? UUID.randomUUID().toString()
                : request.getSessionId().trim();

        List<LlmMessage> history = loadHistory(sessionId);
        history.add(new LlmMessage("user", request.getMessage().trim()));

        List<LlmMessage> payload = new ArrayList<>();
        payload.add(new LlmMessage("system", SYSTEM_PROMPT));
        payload.addAll(trimHistory(history));

        String reply = callLlm(payload);

        history.add(new LlmMessage("assistant", reply));
        saveHistory(sessionId, trimHistory(history));

        return ChatResponse.builder()
                .sessionId(sessionId)
                .reply(reply)
                .build();
    }

    private String callLlm(List<LlmMessage> messages) {
        LlmChatRequest body = LlmChatRequest.builder()
                .model(llmModel)
                .messages(messages)
                .temperature(llmTemperature)
                .maxTokens(llmMaxTokens)
                .build();

        try {
            LlmChatApiResponse response = restClient.post()
                    .uri(llmApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + llmApiKey)
                    .body(body)
                    .retrieve()
                    .body(LlmChatApiResponse.class);

            if (response == null
                    || response.getChoices() == null
                    || response.getChoices().isEmpty()
                    || response.getChoices().get(0).getMessage() == null
                    || response.getChoices().get(0).getMessage().getContent() == null) {
                throw new IllegalStateException("LLM returned an empty response");
            }

            return response.getChoices().get(0).getMessage().getContent().trim();
        } catch (RestClientException e) {
            log.error("LLM call failed", e);
            throw new IllegalStateException("AI service is temporarily unavailable. Please try again later.");
        }
    }

    private List<LlmMessage> loadHistory(String sessionId) {
        String raw = redisService.get(HISTORY_PREFIX + sessionId);
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<List<LlmMessage>>() {});
        } catch (Exception e) {
            log.warn("Cannot parse chat history for session {}", sessionId);
            return new ArrayList<>();
        }
    }

    private void saveHistory(String sessionId, List<LlmMessage> history) {
        try {
            String raw = objectMapper.writeValueAsString(history);
            redisService.save(HISTORY_PREFIX + sessionId, raw, HISTORY_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Cannot save chat history for session {}", sessionId, e);
        }
    }

    private List<LlmMessage> trimHistory(List<LlmMessage> history) {
        if (history.size() <= MAX_HISTORY_MESSAGES) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size()));
    }
}