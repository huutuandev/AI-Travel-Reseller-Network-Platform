package com.aitravel.reseller.controller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.JoinCampaignResponse;
import com.aitravel.reseller.service.CampaignService;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reseller/campaigns")
@RequiredArgsConstructor
@Tag(name = "Reseller Campaign", description = "API cho Reseller quản lý chiến dịch")
public class CampaignController {

    private final CampaignService campaignService;

    @Operation(
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping("/{campaignId}/join")
    public ResponseEntity<ApiResponse<JoinCampaignResponse>> joinCampaign(
            @Parameter(description = "ID của chiến dịch", required = true)
            @PathVariable UUID campaignId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();

        JoinCampaignResponse response =
                campaignService.joinCampaign(campaignId, userEmail);

        return ResponseEntity.ok(
                ApiResponse.success(response.getMessage(), response)
        );
    }
}
