package com.aitravel.reseller.controller.reseller;

import com.aitravel.reseller.dto.request.reseller.RegisterSocialVideoRequest;
import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.reseller.SocialVideoResponse;
import com.aitravel.reseller.service.reseller.ResellerSocialVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reseller/social-videos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESELLER')")
@Tag(name = "Reseller social videos")
@SecurityRequirement(name = "BearerAuth")
public class ResellerSocialVideoController {
    private final ResellerSocialVideoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a TikTok or Facebook video for an approved campaign")
    public ApiResponse<SocialVideoResponse> register(@Valid @RequestBody RegisterSocialVideoRequest request) {
        return ApiResponse.success("Social video registered; metrics are pending provider integration",
                service.register(request));
    }
}
