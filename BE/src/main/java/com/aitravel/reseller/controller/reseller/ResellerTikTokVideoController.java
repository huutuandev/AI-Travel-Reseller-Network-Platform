package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.TikTokVideoListResponse;
import com.aitravel.reseller.service.reseller.TikTokVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reseller/social-accounts/tiktok")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESELLER')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Reseller social accounts")
public class ResellerTikTokVideoController {
    private final TikTokVideoService service;

    @GetMapping("/videos")
    @Operation(summary = "List or find videos for the reseller's active TikTok account")
    public ApiResponse<TikTokVideoListResponse> listCurrentAccountVideos(
            @RequestParam(defaultValue = "20") int maxCount,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String videoId,
            @RequestParam(required = false) UUID socialAccountId) {
        if (socialAccountId != null) {
            if (videoId != null && !videoId.isBlank()) {
                return ApiResponse.success("TikTok video retrieved",
                        service.findVideoPage(socialAccountId, videoId));
            }
            return ApiResponse.success("TikTok videos retrieved",
                    service.listVideos(socialAccountId, maxCount, cursor));
        }
        if (videoId != null && !videoId.isBlank()) {
            return ApiResponse.success("TikTok video retrieved", service.findCurrentResellerVideoPage(videoId));
        }
        return ApiResponse.success("TikTok videos retrieved", service.listCurrentResellerVideos(maxCount, cursor));
    }

    @GetMapping("/{socialAccountId}/videos")
    @Operation(summary = "List videos and available metrics for a linked TikTok account")
    public ApiResponse<TikTokVideoListResponse> listVideos(
            @PathVariable UUID socialAccountId,
            @RequestParam(defaultValue = "20") int maxCount,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) String videoId) {
        if (videoId != null && !videoId.isBlank()) {
            return ApiResponse.success("TikTok video retrieved", service.findVideoPage(socialAccountId, videoId));
        }
        return ApiResponse.success("TikTok videos retrieved", service.listVideos(socialAccountId, maxCount, cursor));
    }
}
