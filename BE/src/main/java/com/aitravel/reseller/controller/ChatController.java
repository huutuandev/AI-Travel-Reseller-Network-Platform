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
@Tag(name = "AI Chatbot", description = "Tư vấn sản phẩm du lịch theo tracking link")
public class ChatController {

    private final ChatService chatService;

    @Operation(
        summary = "Gửi tin nhắn cho chatbot",
        description = """
                Bắt đầu phiên mới bằng cách để trống sessionId và truyền trackingLink.
                Khi tiếp tục, gửi sessionId được trả về cùng message.
                trackingLink không cần gửi lại; nếu có gửi, backend sẽ kiểm tra link
                có khớp với ngữ cảnh của session hay không.
                """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }
}