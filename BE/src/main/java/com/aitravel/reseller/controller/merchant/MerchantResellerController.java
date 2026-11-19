package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantResellerDtos;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantResellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant resellers")
@SecurityRequirement(name = "BearerAuth")
public class MerchantResellerController {
    private final MerchantResellerService service;

    @GetMapping("/resellers")
    @Operation(
            summary = "List resellers associated with the current merchant",
            description = "Returns distinct resellers with their campaign applications in the current merchant."
    )
    public ApiResponse<MerchantResellerDtos.PageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID campaignId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Resellers retrieved",
                service.list(page, size, search, status, campaignId, user)
        );
    }

    @GetMapping("/resellers/{resellerId}")
    @Operation(
            summary = "Get reseller details",
            description = "Returns the reseller profile and campaign applications visible to the current merchant."
    )
    public ApiResponse<MerchantResellerDtos.ResellerDetail> get(
            @PathVariable UUID resellerId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Reseller retrieved",
                service.get(resellerId, user)
        );
    }

    @PatchMapping("/campaign-applications/{applicationId}/status")
    @Operation(
            summary = "Approve, reject, suspend, or restore a reseller's campaign application"
    )
    public ApiResponse<MerchantResellerDtos.CampaignParticipation>
    changeApplicationStatus(
            @PathVariable UUID applicationId,
            @Valid @RequestBody MerchantResellerDtos.StatusRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Campaign application status updated",
                service.changeApplicationStatus(
                        applicationId,
                        request,
                        user
                )
        );
    }
}