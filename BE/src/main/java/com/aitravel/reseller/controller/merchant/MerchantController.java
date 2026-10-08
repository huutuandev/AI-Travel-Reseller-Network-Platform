package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.request.RegisterMerchantRequest;
import com.aitravel.reseller.dto.request.UpdateMerchantRequest;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.MerchantResponse;
import com.aitravel.reseller.dto.respone.ProductResponse;
import com.aitravel.reseller.service.MerchantService;
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
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@Tag(name = "Merchant", description = "APIs for Merchant onboarding, profile, and merchant product operations")
public class MerchantController {

    private final MerchantService merchantService;
    private final ProductService productService;

    @Operation(summary = "Register merchant profile", description = "Onboards the authenticated user as a merchant.", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<MerchantResponse>> registerMerchant(
            @Valid @RequestBody RegisterMerchantRequest request) {
        MerchantResponse response = merchantService.registerMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Merchant profile registered successfully", response));
    }

    @Operation(summary = "Get current merchant profile", description = "Retrieves profile of the authenticated user's merchant.", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MerchantResponse>> getCurrentMerchant() {
        MerchantResponse response = merchantService.getCurrentMerchant();
        return ResponseEntity.ok(ApiResponse.success("Merchant profile retrieved successfully", response));
    }

    @Operation(summary = "Update current merchant profile", description = "Updates business profile of the authenticated user's merchant.", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<MerchantResponse>> updateCurrentMerchant(
            @Valid @RequestBody UpdateMerchantRequest request) {
        MerchantResponse response = merchantService.updateCurrentMerchant(request);
        return ResponseEntity.ok(ApiResponse.success("Merchant profile updated successfully", response));
    }

    @Operation(summary = "Get current merchant products", description = "Retrieves paginated products of the authenticated user's merchant.", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me/products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyProducts(Pageable pageable) {
        Page<ProductResponse> products = productService.getMyProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Merchant products retrieved successfully", products));
    }

    @Operation(summary = "Get merchant by ID", description = "Retrieves public details of a merchant by ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantResponse>> getMerchantById(@PathVariable UUID id) {
        MerchantResponse response = merchantService.getMerchantById(id);
        return ResponseEntity.ok(ApiResponse.success("Merchant details retrieved successfully", response));
    }
}
