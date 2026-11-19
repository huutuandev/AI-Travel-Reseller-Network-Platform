package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantReportDtos;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant reports")
@SecurityRequirement(name = "BearerAuth")
public class MerchantReportController {
    private final MerchantReportService service;

    @GetMapping("/campaigns")
    @Operation(
            summary = "Get campaign performance reports",
            description = """
                    Click counts are lifetime totals because the database stores
                    only a cumulative click_count. The from/to filters apply to
                    booking creation time and commission creation time.
                    """
    )
    public ApiResponse<MerchantReportDtos.CampaignPageResponse>
    campaignReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @Parameter(
                    description = "DRAFT, PENDING_REVIEW, ACTIVE, PAUSED, ENDED, or CANCELLED"
            )
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) UUID campaignId,
            @Parameter(description = "ISO-8601 timestamp, inclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "ISO-8601 timestamp, exclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Campaign reports retrieved",
                service.campaignReport(
                        page,
                        size,
                        search,
                        status,
                        productId,
                        campaignId,
                        from,
                        to,
                        user
                )
        );
    }

    @GetMapping("/resellers")
    @Operation(
            summary = "Get reseller performance reports",
            description = """
                    Returns reseller performance within the current merchant.
                    Click counts are lifetime totals. The from/to filters apply
                    to booking creation time and commission creation time.
                    """
    )
    public ApiResponse<MerchantReportDtos.ResellerPageResponse>
    resellerReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID campaignId,
            @Parameter(description = "ISO-8601 timestamp, inclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "ISO-8601 timestamp, exclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Reseller reports retrieved",
                service.resellerReport(
                        page,
                        size,
                        search,
                        campaignId,
                        from,
                        to,
                        user
                )
        );
    }
}