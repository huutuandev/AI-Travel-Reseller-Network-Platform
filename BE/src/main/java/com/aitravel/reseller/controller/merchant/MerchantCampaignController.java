package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.request.merchant.CampaignDtos;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantCampaignService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/merchant/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant campaigns")
@SecurityRequirement(name = "BearerAuth")
public class MerchantCampaignController {
    private final MerchantCampaignService service;

    @GetMapping
    @Operation(summary = "List campaigns belonging to the current merchant")
    public ApiResponse<CampaignDtos.CampaignPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID productId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Campaigns retrieved",
                service.list(page, size, search, status, productId, user)
        );
    }

    @PostMapping
    @Operation(summary = "Create a campaign")
    public ResponseEntity<ApiResponse<CampaignDtos.CampaignResponse>> create(
            @Valid @RequestBody CampaignDtos.UpsertRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CampaignDtos.CampaignResponse campaign =
                service.create(request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campaign created", campaign));
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Get campaign details")
    public ApiResponse<CampaignDtos.CampaignResponse> get(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Campaign retrieved",
                service.get(campaignId, user)
        );
    }

    @PutMapping("/{campaignId}")
    @Operation(summary = "Update a campaign")
    public ApiResponse<CampaignDtos.CampaignResponse> update(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignDtos.UpsertRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Campaign updated",
                service.update(campaignId, request, user)
        );
    }

    @PatchMapping("/{campaignId}/status")
    @Operation(summary = "Activate, pause, end, or cancel a campaign")
    public ApiResponse<CampaignDtos.CampaignResponse> changeStatus(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignDtos.StatusRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Campaign status updated",
                service.changeStatus(campaignId, request, user)
        );
    }
}