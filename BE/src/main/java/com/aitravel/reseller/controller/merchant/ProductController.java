package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.request.CreateProductRequest;
import com.aitravel.reseller.dto.request.UpdateProductRequest;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.ProductResponse;
import com.aitravel.reseller.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "APIs for viewing and managing products")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create product", description = "Creates a new product for the authenticated user with a merchant profile.", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @Operation(summary = "Get public active products", description = "Retrieves paginated list of active public products.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getPublicProducts(Pageable pageable) {
        Page<ProductResponse> products = productService.getPublicProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Public products retrieved successfully", products));
    }

    @Operation(summary = "Get product by ID", description = "Retrieves details of a product by ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", response));
    }

    @Operation(summary = "Update product", description = "Updates a product owned by the authenticated merchant.", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @Operation(summary = "Delete product", description = "Soft-deletes a product owned by the authenticated merchant (sets status to ARCHIVED).", security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
