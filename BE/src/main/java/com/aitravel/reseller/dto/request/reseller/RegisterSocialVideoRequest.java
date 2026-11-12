package com.aitravel.reseller.dto.request.reseller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterSocialVideoRequest(
        @NotBlank String postUrl,
        @NotNull UUID socialAccountId,
        @NotNull UUID campaignApplicationId) {
}
