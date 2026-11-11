package com.aitravel.reseller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @Schema(
            description = "Session id. Leave empty to start a new chat; reuse the returned sessionId to continue.",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    )
    private String sessionId;

    @Schema(
            description = "Tracking code from tracking_links.tracking_code. Required for a new session. " +
                    "Optional when continuing a session; if supplied, it must match that session's original tracking code.",
            example = "MOCK-BANA-001"
    )
    @Size(max = 100, message = "Tracking link must be at most 100 characters")
    private String trackingLink;

    @Schema(
            description = "Customer message",
            example = "Cuối tuần này đi Đà Nẵng với gia đình thì nên chọn vé nào?"
    )
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must be at most 2000 characters")
    private String message;
}