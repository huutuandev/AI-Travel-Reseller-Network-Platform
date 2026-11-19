package com.aitravel.reseller.dto.respone.merchant;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MerchantResellerDtos {
    private MerchantResellerDtos() {}

    @Schema(name = "MerchantResellerPageResponse")
    public record PageResponse(
            List<ResellerSummary> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    @Schema(name = "MerchantResellerSummary")
    public record ResellerSummary(
            UUID resellerId,
            String fullName,
            String email,
            String phone,
            String resellerProfileStatus,
            Instant resellerSince,
            long campaignCount,
            List<CampaignParticipation> campaignApplications
    ) {}

    @Schema(name = "MerchantResellerDetail")
    public record ResellerDetail(
            UUID resellerId,
            String fullName,
            String email,
            String phone,
            String resellerProfileStatus,
            Instant resellerSince,
            List<CampaignParticipation> campaignApplications
    ) {}

    @Schema(name = "MerchantResellerIdentity")
    public record ResellerIdentity(
            UUID resellerId,
            String fullName,
            String email,
            String phone,
            String resellerProfileStatus,
            Instant resellerSince
    ) {}

    @Schema(name = "MerchantCampaignParticipation")
    public record CampaignParticipation(
            UUID applicationId,
            UUID resellerId,
            UUID campaignId,
            String campaignTitle,
            UUID productId,
            String productTitle,
            String campaignStatus,
            String applicationStatus,
            String commissionType,
            java.math.BigDecimal commissionRate,
            Instant appliedAt,
            Instant reviewedAt
    ) {}

    @Schema(name = "MerchantResellerApplicationStatusRequest")
    public record StatusRequest(
            @Schema(
                    description = "Trạng thái đích",
                    allowableValues = {"APPROVED", "REJECTED", "SUSPENDED"}
            )
            String status
    ) {}
}