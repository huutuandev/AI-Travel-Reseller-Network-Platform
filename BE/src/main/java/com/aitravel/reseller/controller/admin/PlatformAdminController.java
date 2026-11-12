package com.aitravel.reseller.controller.admin;

import com.aitravel.reseller.dto.request.admin.AdminRequests;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.admin.AdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Platform Admin")
@SecurityRequirement(name = "BearerAuth")
public class PlatformAdminController {
    private final AdminService service;

    @GetMapping("/tenants")
    public ApiResponse<AdminService.PageResult> tenants(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size, @RequestParam(defaultValue="") String search,
            @RequestParam(defaultValue="") String status) {
        return ApiResponse.success("Tenants", service.tenants(page,size,search,status));
    }

    @GetMapping("/tenants/{id}")
    public ApiResponse<Map<String,Object>> tenant(@PathVariable UUID id) {
        return ApiResponse.success("Tenant", service.tenant(id));
    }

    @PatchMapping("/tenants/{id}/status")
    public ApiResponse<Map<String,Object>> status(@PathVariable UUID id,
            @Valid @RequestBody AdminRequests.StatusChange request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success("Tenant status updated", service.changeStatus(id,request,user.getId()));
    }

    @GetMapping("/tenants/{id}/logs")
    public ApiResponse<AdminService.PageResult> tenantLogs(@PathVariable UUID id,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success("Tenant events", service.logs(id,page,size,false));
    }

    @GetMapping("/dashboard/overview")
    public ApiResponse<Map<String,Object>> overview(@RequestParam(defaultValue="30") int days) {
        return ApiResponse.success("Platform overview", service.overview(days));
    }

    @GetMapping("/dashboard/recent-activities")
    public ApiResponse<AdminService.PageResult> activities(@RequestParam(defaultValue="10") int limit) {
        return ApiResponse.success("Recent tenant events", service.logs(null,0,limit,true));
    }

    @GetMapping("/system/config")
    public ApiResponse<Map<String,Object>> config() {
        return ApiResponse.success("Preview settings", service.settings());
    }

    @PutMapping("/system/config")
    public ApiResponse<Map<String,Object>> config(@Valid @RequestBody AdminRequests.SettingsChange request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success("Preview settings saved", service.settings(request,user.getId()));
    }

    @GetMapping("/system/logs")
    public ApiResponse<AdminService.PageResult> logs(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        return ApiResponse.success("Platform events", service.logs(null,page,size,false));
    }
}
