package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantFinanceDtos;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantCommissionPayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant commission and payout")
@SecurityRequirement(name = "BearerAuth")
public class MerchantCommissionPayoutController {
    private final MerchantCommissionPayoutService service;

    @GetMapping("/commissions")
    @Operation(summary = "List commissions belonging to the current merchant")
    public ApiResponse<MerchantFinanceDtos.CommissionPage> listCommissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID resellerId,
            @RequestParam(required = false) UUID campaignId,
            @RequestParam(required = false) UUID bookingId,
            @Parameter(description = "ISO-8601 timestamp, inclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "ISO-8601 timestamp, exclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Commissions retrieved",
                service.listCommissions(
                        page,
                        size,
                        status,
                        resellerId,
                        campaignId,
                        bookingId,
                        from,
                        to,
                        user
                )
        );
    }

    @GetMapping("/commissions/{commissionId}")
    @Operation(summary = "Get commission details")
    public ApiResponse<MerchantFinanceDtos.CommissionItem> getCommission(
            @PathVariable UUID commissionId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Commission retrieved",
                service.getCommission(commissionId, user)
        );
    }

    @PatchMapping("/commissions/{commissionId}/status")
    @Operation(
            summary = "Approve, make payable, cancel, or reverse a commission",
            description = """
                    PENDING -> APPROVED.
                    APPROVED -> PAYABLE only when its booking is COMPLETED.
                    Cancellation requires a CANCELLED booking.
                    Reversal requires a REFUNDED booking.
                    Commission status PAID is set only when its payout is completed.
                    """
    )
    public ApiResponse<MerchantFinanceDtos.CommissionItem>
    changeCommissionStatus(
            @PathVariable UUID commissionId,
            @Valid @RequestBody
            MerchantFinanceDtos.CommissionStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Commission status updated",
                service.changeCommissionStatus(commissionId, request, user)
        );
    }

    @GetMapping("/payouts")
    @Operation(summary = "List payouts belonging to the current merchant")
    public ApiResponse<MerchantFinanceDtos.PayoutPage> listPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID resellerId,
            @Parameter(description = "ISO-8601 timestamp, inclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "ISO-8601 timestamp, exclusive")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Payouts retrieved",
                service.listPayouts(
                        page,
                        size,
                        status,
                        resellerId,
                        from,
                        to,
                        user
                )
        );
    }

    @GetMapping("/payouts/{payoutId}")
    @Operation(summary = "Get payout details and commission items")
    public ApiResponse<MerchantFinanceDtos.PayoutDetail> getPayout(
            @PathVariable UUID payoutId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Payout retrieved",
                service.getPayout(payoutId, user)
        );
    }

    @PostMapping("/payouts")
    @Operation(
            summary = "Create a payout batch from payable commissions",
            description = """
                    All selected commissions must belong to the current merchant
                    and the same reseller and currency, be PAYABLE, and not
                    already belong to another payout.
                    """
    )
    public ApiResponse<MerchantFinanceDtos.PayoutDetail> createPayout(
            @Valid @RequestBody
            MerchantFinanceDtos.CreatePayoutRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Payout created",
                service.createPayout(request, user)
        );
    }

    @PatchMapping("/payouts/{payoutId}/status")
    @Operation(
            summary = "Record payout processing result",
            description = """
                    PROCESSING -> COMPLETED or FAILED.
                    FAILED -> PROCESSING retries the same payout batch.
                    Mark COMPLETED only after the external payment has succeeded.
                    """
    )
    public ApiResponse<MerchantFinanceDtos.PayoutDetail> changePayoutStatus(
            @PathVariable UUID payoutId,
            @Valid @RequestBody
            MerchantFinanceDtos.PayoutStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return ApiResponse.success(
                "Payout status updated",
                service.changePayoutStatus(payoutId, request, user)
        );
    }

    @GetMapping(
            value = "/payouts/{payoutId}/export",
            produces = "text/csv"
    )
    @Operation(summary = "Export payout and commission items as CSV")
    public ResponseEntity<byte[]> exportPayout(
            @PathVariable UUID payoutId,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        byte[] csv = service.exportPayoutCsv(payoutId, user);

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"payout-" + payoutId + ".csv\""
                )
                .body(csv);
    }
}