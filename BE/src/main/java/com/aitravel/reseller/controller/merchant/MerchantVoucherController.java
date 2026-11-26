package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.request.merchant.VoucherDtos;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantVoucherService;
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
@RequestMapping("/api/v1/merchant/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant vouchers")
@SecurityRequirement(name = "BearerAuth")
public class MerchantVoucherController {
    private final MerchantVoucherService service;

    @GetMapping
    @Operation(summary = "List vouchers belonging to the current merchant")
    public ApiResponse<VoucherDtos.PageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID campaignApplicationId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Vouchers retrieved",
                service.list(
                        page,
                        size,
                        search,
                        status,
                        campaignApplicationId,
                        user
                )
        );
    }

    @GetMapping("/{voucherId}")
    @Operation(summary = "Get voucher details")
    public ApiResponse<VoucherDtos.VoucherResponse> get(
            @PathVariable UUID voucherId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Voucher retrieved",
                service.get(voucherId, user)
        );
    }

    @PostMapping
    @Operation(summary = "Create a voucher")
    public ApiResponse<VoucherDtos.VoucherResponse> create(
            @Valid @RequestBody VoucherDtos.UpsertRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Voucher created",
                service.create(request, user)
        );
    }

    @PutMapping("/{voucherId}")
    @Operation(summary = "Update a draft voucher")
    public ApiResponse<VoucherDtos.VoucherResponse> update(
            @PathVariable UUID voucherId,
            @Valid @RequestBody VoucherDtos.UpsertRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Voucher updated",
                service.update(voucherId, request, user)
        );
    }

    @PatchMapping("/{voucherId}/status")
    @Operation(summary = "Activate or disable a voucher")
    public ApiResponse<VoucherDtos.VoucherResponse> changeStatus(
            @PathVariable UUID voucherId,
            @Valid @RequestBody VoucherDtos.StatusRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Voucher status updated",
                service.changeStatus(voucherId, request, user)
        );
    }
}