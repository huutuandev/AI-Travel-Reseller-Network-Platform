package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.request.merchant.ProductDtos;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/merchant/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant products")
@SecurityRequirement(name = "BearerAuth")
public class MerchantProductController {
    private final MerchantProductService service;

    @GetMapping
    @Operation(summary = "List products belonging to the current merchant")
    public ApiResponse<ProductDtos.ProductPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Products retrieved",
                service.list(page, size, search, status, user)
        );
    }

    @PostMapping
    @Operation(summary = "Create a product")
    public ResponseEntity<ApiResponse<ProductDtos.ProductResponse>> create(
            @Valid @RequestBody ProductDtos.UpsertRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ProductDtos.ProductResponse product = service.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", product));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get product details")
    public ApiResponse<ProductDtos.ProductResponse> get(
            @PathVariable UUID productId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success("Product retrieved", service.get(productId, user));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update a product")
    public ApiResponse<ProductDtos.ProductResponse> update(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductDtos.UpsertRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success("Product updated", service.update(productId, request, user));
    }

    @PatchMapping("/{productId}/availability")
    @Operation(summary = "Change product availability status")
    public ApiResponse<ProductDtos.ProductResponse> changeAvailability(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductDtos.AvailabilityRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Product availability updated",
                service.changeAvailability(productId, request, user)
        );
    }
}