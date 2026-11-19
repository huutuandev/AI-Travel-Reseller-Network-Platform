package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.ResellerScoreRankingResponse;
import com.aitravel.reseller.service.merchant.ResellerScoreRankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/resellers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Merchant")
public class ResellerScoreRankingController {
    private final ResellerScoreRankingService service;

    @GetMapping("/score-ranking")
    @Operation(summary = "Rank resellers by the engagement of all videos on their connected TikTok accounts")
    public ApiResponse<ResellerScoreRankingResponse> ranking() {
        return ApiResponse.success("Reseller score ranking", service.ranking());
    }
}
