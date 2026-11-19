package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.ResellerDashboardResponse;
import com.aitravel.reseller.service.reseller.ResellerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reseller/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESELLER')")
@Tag(name = "Reseller dashboard")
@SecurityRequirement(name = "BearerAuth")
public class ResellerDashboardController {

    private final ResellerDashboardService service;

    @GetMapping
    @Operation(summary = "Get dashboard data for the authenticated reseller")
    public ApiResponse<ResellerDashboardResponse> getDashboard() {
        return ApiResponse.success("Reseller dashboard retrieved successfully", service.getDashboard());
    }
}
