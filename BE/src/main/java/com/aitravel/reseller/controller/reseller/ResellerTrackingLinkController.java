package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.TrackingLinkResponse;
import com.aitravel.reseller.service.reseller.ResellerTrackingLinkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reseller/tracking-links")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESELLER')")
@Tag(name = "Reseller tracking links")
@SecurityRequirement(name = "BearerAuth")
public class ResellerTrackingLinkController {

    private final ResellerTrackingLinkService service;

    @GetMapping
    @Operation(summary = "Get reseller tracking links")
    public ApiResponse<List<TrackingLinkResponse>> getMyTrackingLinks() {

        return ApiResponse.success(
                "Tracking links retrieved successfully",
                service.getMyTrackingLinks());
    }
}