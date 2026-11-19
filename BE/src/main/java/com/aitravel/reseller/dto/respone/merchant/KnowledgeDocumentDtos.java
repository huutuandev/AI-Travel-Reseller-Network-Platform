package com.aitravel.reseller.dto.respone.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class KnowledgeDocumentDtos {
    private KnowledgeDocumentDtos() {}

    @Schema(name = "MerchantKnowledgeDocumentListItem")
    public record ListItem(
            UUID id,
            UUID productId,
            String title,
            String status,
            boolean fileUploaded,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @Schema(name = "MerchantKnowledgeDocumentDetail")
    public record Detail(
            UUID id,
            UUID productId,
            String title,
            String status,
            boolean fileUploaded,
            String originalFileName,
            String mediaType,
            Long fileSize,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @Schema(name = "MerchantKnowledgeDocumentPageResponse")
    public record PageResponse(
            List<ListItem> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    @Schema(name = "MerchantKnowledgeDocumentUpdateRequest")
    public record UpdateRequest(
            @NotBlank
            @Size(max = 255)
            String title,

            @Schema(
                    description = "Product ID tùy chọn. Gửi null để bỏ liên kết Product."
            )
            UUID productId
    ) {}
}