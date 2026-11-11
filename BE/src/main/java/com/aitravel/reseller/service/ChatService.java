package com.aitravel.reseller.service;

import com.aitravel.reseller.dto.chat.ChatSessionContext;
import com.aitravel.reseller.dto.llm.LlmChatApiResponse;
import com.aitravel.reseller.dto.llm.LlmChatRequest;
import com.aitravel.reseller.dto.llm.LlmMessage;
import com.aitravel.reseller.dto.request.ChatRequest;
import com.aitravel.reseller.dto.respone.ChatResponse;
import com.aitravel.reseller.entity.KnowledgeDocument;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.KnowledgeDocumentRepository;
import com.aitravel.reseller.service.chat.ChatTrackingContextResolver;
import com.aitravel.reseller.service.chat.KnowledgeEmbeddingService;
import com.aitravel.reseller.service.chat.KnowledgeIndexingService;
import com.aitravel.reseller.service.chat.KnowledgeVectorRepository;
import com.aitravel.reseller.service.chat.KnowledgeVectorRepository.KnowledgeChunkMatch;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ChatService {

    private static final String SESSION_CONTEXT_PREFIX = "chat:session:context:";
    private static final String HISTORY_PREFIX = "chat:session:messages:";
    private static final long SESSION_TTL_MINUTES = 30;
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final String ACTIVE = "ACTIVE";

    private static final String NO_INFORMATION_REPLY =
            "Hiện tại tôi chưa có thông tin phù hợp trong tài liệu về sản phẩm này. "
                    + "Bạn vui lòng liên hệ doanh nghiệp để được hỗ trợ thêm.";

    private static final String SYSTEM_PROMPT = """
            Bạn là AI tư vấn sản phẩm du lịch.
            Trả lời bằng tiếng Việt, ngắn gọn và thân thiện.

            Chỉ sử dụng thông tin trong phần TÀI LIỆU THAM KHẢO được cung cấp ở cuối chỉ dẫn.
            Không dùng kiến thức bên ngoài để bổ sung chi tiết về sản phẩm.
            Không tự suy đoán giá, tình trạng chỗ, khuyến mãi, thời gian hoạt động,
            điều kiện đổi trả hoặc bất kỳ thông tin nào không có trong tài liệu.
            Nếu tài liệu không trả lời được câu hỏi, hãy nói rõ rằng hiện chưa có thông tin.
            Không nhận thanh toán và không hướng khách chuyển tiền cho bạn.
            Khi khách muốn đặt, hướng khách tới quy trình hoặc đường dẫn đặt hàng chính thức
            nếu thông tin đó có trong tài liệu.
            Không nhận mình là nhân viên của doanh nghiệp.
            """;

    private final RedisService redisService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;
    private final ChatTrackingContextResolver trackingContextResolver;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeIndexingService knowledgeIndexingService;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final KnowledgeVectorRepository knowledgeVectorRepository;

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

    @Value("${ai.rag.top-k:5}")
    private int ragTopK;

    @Value("${ai.rag.minimum-similarity:0.50}")
    private double minimumSimilarity;

    public ChatService(
            RedisService redisService,
            ChatTrackingContextResolver trackingContextResolver,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeIndexingService knowledgeIndexingService,
            KnowledgeEmbeddingService knowledgeEmbeddingService,
            KnowledgeVectorRepository knowledgeVectorRepository
    ) {
        this.redisService = redisService;
        this.restClient = RestClient.create();
        this.trackingContextResolver = trackingContextResolver;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeIndexingService = knowledgeIndexingService;
        this.knowledgeEmbeddingService = knowledgeEmbeddingService;
        this.knowledgeVectorRepository = knowledgeVectorRepository;
    }

    public ChatResponse chat(ChatRequest request) {
        if (llmApiKey == null || llmApiKey.isBlank()) {
            throw new IllegalStateException("LLM API key is not configured.");
        }

        String message = request.getMessage().trim();
        boolean newSession = request.getSessionId() == null
                || request.getSessionId().isBlank();

        String sessionId;
        ChatSessionContext context;

        if (newSession) {
            if (request.getTrackingLink() == null || request.getTrackingLink().isBlank()) {
                throw new IllegalArgumentException(
                        "Tracking link is required when starting a new chat session.");
            }

            context = trackingContextResolver.resolve(request.getTrackingLink());
            sessionId = UUID.randomUUID().toString();

            // Context được tạo từ tracking link đã xác thực ở backend.
            saveContext(sessionId, context);
        } else {
            sessionId = validateSessionId(request.getSessionId());
            context = loadContext(sessionId);

            // Nếu client gửi tracking link khi tiếp tục, xác minh nó khớp session.
            if (request.getTrackingLink() != null && !request.getTrackingLink().isBlank()) {
                ChatSessionContext suppliedContext =
                        trackingContextResolver.resolve(request.getTrackingLink());

                if (!sameScope(context, suppliedContext)) {
                    throw new IllegalArgumentException(
                            "Tracking link does not match this chat session.");
                }
            }
        }

        // Kiểm tra lại tài liệu thuộc merchant/product trong context trước khi dùng vector.
        validateCurrentDocumentScope(context);

        // Chỉ tạo embedding khi tài liệu chưa có vector.
        knowledgeIndexingService.ensureIndexed(context);

        List<Double> queryEmbedding = knowledgeEmbeddingService.embedQuery(message);
        List<KnowledgeChunkMatch> matches = knowledgeVectorRepository.findNearest(
                context.getKnowledgeDocumentIds(),
                queryEmbedding,
                ragTopK
        );

        List<KnowledgeChunkMatch> relevantMatches = matches.stream()
                .filter(match -> match.similarity() >= minimumSimilarity)
                .toList();

        List<LlmMessage> history = loadHistory(sessionId);
        history.add(new LlmMessage("user", message));

        String reply;
        if (relevantMatches.isEmpty()) {
            // Không gọi LLM nếu không có đoạn tài liệu đủ liên quan.
            reply = NO_INFORMATION_REPLY;
        } else {
            String systemPrompt = buildSystemPrompt(relevantMatches);
            List<LlmMessage> payload = new ArrayList<>();
            payload.add(new LlmMessage("system", systemPrompt));
            payload.addAll(trimHistory(history));
            reply = callLlm(payload);
        }

        history.add(new LlmMessage("assistant", reply));
        saveHistory(sessionId, trimHistory(history));
        saveContext(sessionId, context);

        return ChatResponse.builder()
                .sessionId(sessionId)
                .reply(reply)
                .build();
    }

    private String validateSessionId(String value) {
        String sessionId = value.trim();

        try {
            UUID parsed = UUID.fromString(sessionId);
            if (!parsed.toString().equalsIgnoreCase(sessionId)) {
                throw new IllegalArgumentException("Invalid chat session id.");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid chat session id.");
        }

        return sessionId;
    }

    private ChatSessionContext loadContext(String sessionId) {
        String raw = redisService.get(SESSION_CONTEXT_PREFIX + sessionId);
        if (raw == null || raw.isBlank()) {
            throw new ResourceNotFoundException(
                    "Chat session was not found or has expired. Start a new chat session.");
        }

        try {
            ChatSessionContext context =
                    objectMapper.readValue(raw, ChatSessionContext.class);

            if (context.getMerchantId() == null
                    || context.getProductId() == null
                    || context.getKnowledgeDocumentIds() == null
                    || context.getKnowledgeDocumentIds().isEmpty()) {
                throw new ResourceNotFoundException(
                        "Chat session does not contain a valid product knowledge scope.");
            }

            return context;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Cannot read chat context for session {}", sessionId, e);
            throw new ResourceNotFoundException(
                    "Chat session was not found or has expired. Start a new chat session.");
        }
    }

    private void saveContext(String sessionId, ChatSessionContext context) {
        try {
            String raw = objectMapper.writeValueAsString(context);
            redisService.save(
                    SESSION_CONTEXT_PREFIX + sessionId,
                    raw,
                    SESSION_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.error("Cannot save chat context for session {}", sessionId, e);
            throw new IllegalStateException("Unable to save chat session.");
        }
    }

    private boolean sameScope(
            ChatSessionContext stored,
            ChatSessionContext supplied
    ) {
        if (!stored.getTrackingCode().equals(supplied.getTrackingCode())
                || !stored.getMerchantId().equals(supplied.getMerchantId())
                || !stored.getProductId().equals(supplied.getProductId())) {
            return false;
        }

        Set<UUID> storedDocumentIds = new HashSet<>(stored.getKnowledgeDocumentIds());
        Set<UUID> suppliedDocumentIds = new HashSet<>(supplied.getKnowledgeDocumentIds());
        return storedDocumentIds.equals(suppliedDocumentIds);
    }

    private void validateCurrentDocumentScope(ChatSessionContext context) {
        List<KnowledgeDocument> currentDocuments =
                knowledgeDocumentRepository.findByMerchantIdAndProductIdAndStatusOrderByIdAsc(
                        context.getMerchantId(),
                        context.getProductId(),
                        ACTIVE
                );

        Set<UUID> currentDocumentIds = new HashSet<>();
        for (KnowledgeDocument document : currentDocuments) {
            if (document.getContent() != null && !document.getContent().isBlank()) {
                currentDocumentIds.add(document.getId());
            }
        }

        if (!currentDocumentIds.containsAll(context.getKnowledgeDocumentIds())) {
            throw new ResourceNotFoundException(
                    "Knowledge documents for this chat session are no longer available.");
        }
    }

    private String buildSystemPrompt(List<KnowledgeChunkMatch> matches) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
        prompt.append("\n\nTÀI LIỆU THAM KHẢO:\n");

        for (int i = 0; i < matches.size(); i++) {
            prompt.append("\n[Đoạn ")
                    .append(i + 1)
                    .append("]\n")
                    .append(matches.get(i).content())
                    .append('\n');
        }

        return prompt.toString();
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
                    || response.getChoices().get(0).getMessage().getContent() == null
                    || response.getChoices().get(0).getMessage().getContent().isBlank()) {
                throw new IllegalStateException("LLM returned an empty response.");
            }

            return response.getChoices().get(0).getMessage().getContent().trim();
        } catch (RestClientException e) {
            log.error("LLM call failed", e);
            throw new IllegalStateException(
                    "AI service is temporarily unavailable. Please try again later.");
        }
    }

    private List<LlmMessage> loadHistory(String sessionId) {
        String raw = redisService.get(HISTORY_PREFIX + sessionId);
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(
                    raw,
                    new TypeReference<List<LlmMessage>>() {}
            );
        } catch (Exception e) {
            log.warn("Cannot parse chat history for session {}", sessionId, e);
            return new ArrayList<>();
        }
    }

    private void saveHistory(String sessionId, List<LlmMessage> history) {
        try {
            String raw = objectMapper.writeValueAsString(history);
            redisService.save(
                    HISTORY_PREFIX + sessionId,
                    raw,
                    SESSION_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.error("Cannot save chat history for session {}", sessionId, e);
            throw new IllegalStateException("Unable to save chat history.");
        }
    }

    private List<LlmMessage> trimHistory(List<LlmMessage> history) {
        if (history.size() <= MAX_HISTORY_MESSAGES) {
            return new ArrayList<>(history);
        }

        return new ArrayList<>(
                history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size())
        );
    }
}