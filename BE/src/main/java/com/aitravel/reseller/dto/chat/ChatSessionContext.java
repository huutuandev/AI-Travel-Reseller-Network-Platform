package com.aitravel.reseller.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionContext {

    private String trackingCode;

    private UUID merchantId;

    private UUID productId;

    @Builder.Default
    private List<UUID> knowledgeDocumentIds = new ArrayList<>();
}