package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.CampaignResponse;
import com.aitravel.reseller.service.reseller.ResellerCampaignService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reseller/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESELLER')")
@Tag(name = "Reseller campaigns")
@SecurityRequirement(name = "BearerAuth")
public class ResellerCampaignController {

    private final ResellerCampaignService service;

    @GetMapping
    @Operation(summary = "Get all campaigns")
    public ApiResponse<List<CampaignResponse>> getAllCampaigns() {

        return ApiResponse.success(
                "Campaigns retrieved successfully",
                service.getAllCampaigns());
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "Get campaign detail")
    public ApiResponse<CampaignResponse> getCampaignDetail(
            @PathVariable UUID campaignId) {

        return ApiResponse.success(
                "Campaign retrieved successfully",
                service.getCampaignDetail(campaignId));
    }
}