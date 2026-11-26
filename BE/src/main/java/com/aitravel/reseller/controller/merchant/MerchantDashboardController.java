package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantDashboardDtos;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/merchant/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant dashboard")
@SecurityRequirement(name = "BearerAuth")
public class MerchantDashboardController {

    private final MerchantDashboardService service;

    @GetMapping("/overview")
    @Operation(
            summary = "Get merchant dashboard overview",
            description = """
                    Returns dashboard summary, booking performance, top campaigns,
                    and recent activity. Clicks are lifetime totals because the
                    database stores only cumulative click_count values.
                    """
    )
    public ApiResponse<MerchantDashboardDtos.Overview> getOverview(
            @Parameter(description = "ISO-8601 timestamp, inclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @Parameter(description = "ISO-8601 timestamp, exclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Merchant dashboard retrieved",
                service.getOverview(user, from, to)
        );
    }
}