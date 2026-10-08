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
            summary = "Tham gia chiến dịch và nhận Tracking Link",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Tham gia chiến dịch thành công hoặc đã tham gia",
                    content = @Content(
                            schema = @Schema(implementation = JoinCampaignResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Không có quyền truy cập / Chưa đăng nhập",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Người dùng không có quyền (không phải RESELLER hoặc bị khóa)",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy chiến dịch",
                    content = @Content
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Chiến dịch không hoạt động",
                    content = @Content
            )
    })
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
