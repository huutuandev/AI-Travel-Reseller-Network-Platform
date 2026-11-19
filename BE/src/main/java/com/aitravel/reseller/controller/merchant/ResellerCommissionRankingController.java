package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.ResellerCommissionRankingResponse;
import com.aitravel.reseller.service.merchant.ResellerCommissionRankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/resellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Merchant")
public class ResellerCommissionRankingController {
    private final ResellerCommissionRankingService service;

    @GetMapping("/commission-ranking")
    @Operation(summary = "Rank resellers by attributed commission and booking count")
    public ApiResponse<ResellerCommissionRankingResponse> ranking(
            @RequestParam(defaultValue = "VND") String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size) {
        return ApiResponse.success("Reseller commission ranking", service.ranking(currency, page, size));
    }
}
