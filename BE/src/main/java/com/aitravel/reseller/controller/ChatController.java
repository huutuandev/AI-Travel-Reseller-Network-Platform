package com.aitravel.reseller.controller;

import com.aitravel.reseller.dto.request.ChatRequest;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.ChatResponse;
import com.aitravel.reseller.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chatbot", description = "Basic customer consultation chatbot")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Send a chat message",
            description = "First call can omit sessionId. Reuse the returned sessionId to continue the conversation.")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }
}