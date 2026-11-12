package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.ApplicationResponse;
import com.aitravel.reseller.dto.respone.reseller.RegisteredCampaignResponse;
import com.aitravel.reseller.service.reseller.ResellerApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reseller/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESELLER')")
@Tag(name = "Reseller applications")
@SecurityRequirement(name = "BearerAuth")
public class ResellerApplicationController {

    private final ResellerApplicationService service;

    @PostMapping("/{campaignId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Apply to a campaign")
    public ApiResponse<ApplicationResponse> apply(
            @PathVariable UUID campaignId) {
        return ApiResponse.success(
                "Application submitted successfully",
                service.apply(campaignId));
    }

    @GetMapping("/applications")
    @Operation(summary = "Get all registered campaigns")
    public ApiResponse<List<RegisteredCampaignResponse>> getMyApplications() {

        return ApiResponse.success(
                "Registered campaigns retrieved successfully",
                service.getMyApplications());
    }
}