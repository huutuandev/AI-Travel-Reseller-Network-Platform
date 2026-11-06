package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.CommissionResponse;
import com.aitravel.reseller.service.reseller.ResellerCommissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reseller/commissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESELLER')")
@Tag(name = "Reseller commissions")
@SecurityRequirement(name = "BearerAuth")
public class ResellerCommissionController {

    private final ResellerCommissionService service;

    @GetMapping
    @Operation(summary = "Get reseller commissions")
    public ApiResponse<List<CommissionResponse>> getMyCommissions() {

        return ApiResponse.success(
                "Commissions retrieved successfully",
                service.getMyCommissions());
    }
}