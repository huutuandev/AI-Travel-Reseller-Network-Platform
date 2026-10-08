package com.aitravel.reseller.controller;

import com.aitravel.reseller.dto.request.CampaignRequest;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.CampaignResponse;
import com.aitravel.reseller.dto.respone.JoinCampaignResponse;
import com.aitravel.reseller.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Campaign", description = "API quản lý và tham gia chiến dịch")
public class CampaignController {

    private final CampaignService campaignService;

    // ----- RESELLER API -----

    @Operation(summary = "Tham gia chiến dịch (Reseller)", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/reseller/campaigns/{campaignId}/join")
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

    // ----- MANAGEMENT API -----

    @Operation(summary = "Tạo chiến dịch mới (Admin/Merchant)", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/management/campaigns")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo chiến dịch thành công", response));
    }

    @Operation(summary = "Cập nhật chiến dịch (Admin/Merchant)", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/management/campaigns/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable UUID id,
            @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chiến dịch thành công", response));
    }

    @Operation(summary = "Lấy thông tin chi tiết chiến dịch (Admin/Merchant)", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/management/campaigns/{id}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(@PathVariable UUID id) {
        CampaignResponse response = campaignService.getCampaignById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chiến dịch thành công", response));
    }

    @Operation(summary = "Lấy danh sách tất cả chiến dịch (Admin/Merchant)", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/management/campaigns")
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getAllCampaigns() {
        List<CampaignResponse> responses = campaignService.getAllCampaigns();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chiến dịch thành công", responses));
    }
}
