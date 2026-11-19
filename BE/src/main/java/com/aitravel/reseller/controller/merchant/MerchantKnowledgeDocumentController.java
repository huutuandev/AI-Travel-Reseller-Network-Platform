package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.merchant.KnowledgeDocumentDtos;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantKnowledgeDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/knowledge-documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant knowledge documents")
@SecurityRequirement(name = "BearerAuth")
public class MerchantKnowledgeDocumentController {
    private final MerchantKnowledgeDocumentService service;

    @GetMapping
    @Operation(summary = "List knowledge documents")
    public ApiResponse<KnowledgeDocumentDtos.PageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID productId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Knowledge documents retrieved",
                service.list(page, size, search, status, productId, user)
        );
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get knowledge document details")
    public ApiResponse<KnowledgeDocumentDtos.Detail> get(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Knowledge document retrieved",
                service.get(documentId, user)
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a PDF or Word document",
            description = "Choose a PDF or .docx file using the file picker."
    )
    public ApiResponse<KnowledgeDocumentDtos.Detail> upload(
            @Parameter(description = "PDF or Word .docx file")
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Knowledge document uploaded",
                service.upload(file, user)
        );
    }

    @PutMapping(
            value = "/{documentId}/file",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Replace an uploaded knowledge document file")
    public ApiResponse<KnowledgeDocumentDtos.Detail> replaceFile(
            @PathVariable UUID documentId,
            @Parameter(description = "Replacement PDF or Word .docx file")
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Knowledge document file replaced",
                service.replaceFile(documentId, file, user)
        );
    }

    @GetMapping("/{documentId}/file")
    @Operation(summary = "Download the original knowledge document file")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        var file = service.download(documentId, user);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.mediaType());
        } catch (RuntimeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition contentDisposition =
                ContentDisposition.attachment()
                        .filename(
                                file.originalFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.bytes().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDisposition.toString()
                )
                .body(new ByteArrayResource(file.bytes()));
    }

    @PutMapping("/{documentId}")
    @Operation(summary = "Update document title or linked product")
    public ApiResponse<KnowledgeDocumentDtos.Detail> update(
            @PathVariable UUID documentId,
            @RequestBody KnowledgeDocumentDtos.UpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Knowledge document updated",
                service.update(documentId, request, user)
        );
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Archive a knowledge document")
    public ApiResponse<KnowledgeDocumentDtos.Detail> archive(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Knowledge document archived",
                service.archive(documentId, user)
        );
    }
}