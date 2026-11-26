package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantBookingDtos;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantBookingService;
import com.aitravel.reseller.dto.request.merchant.MerchantBookingWebhookDtos;
import com.aitravel.reseller.service.merchant.MerchantBookingWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/merchant/bookings")
@RequiredArgsConstructor
@Tag(name = "Merchant bookings")
public class MerchantBookingController {
        private final MerchantBookingService service;
        private final MerchantBookingWebhookService webhookService;

        @PreAuthorize("hasRole('MERCHANT')")
        @SecurityRequirement(name = "BearerAuth")
        @GetMapping
        @Operation(
                summary = "List bookings belonging to the current merchant",
                description = """
                        APPROVE changes booking_status from PENDING to CONFIRMED.
                        REJECT changes booking_status from PENDING to CANCELLED.
                        REVERSE changes booking_status from CONFIRMED or COMPLETED to REFUNDED.
                        Use REVERSE only after the customer has actually received a refund.
                        """
        )
        public ApiResponse<MerchantBookingDtos.PageResponse> list(
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "20") int size,
                @RequestParam(required = false) String search,
                @Parameter(
                        description = "PENDING, CONFIRMED, CANCELLED, COMPLETED, or REFUNDED"
                )
                @RequestParam(required = false) String status,
                @RequestParam(required = false) UUID productId,
                @Parameter(description = "Reseller profile ID")
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
                        "Bookings retrieved",
                        service.list(
                                page,
                                size,
                                search,
                                status,
                                productId,
                                resellerId,
                                from,
                                to,
                                user
                        )
                );
        }

        @PreAuthorize("hasRole('MERCHANT')")
        @SecurityRequirement(name = "BearerAuth")
        @GetMapping("/{bookingId}")
        @Operation(
                summary = "Get booking details",
                description = "Returns a booking only if it belongs to the current merchant."
        )
        public ApiResponse<MerchantBookingDtos.BookingItem> get(
                @PathVariable UUID bookingId,
                @AuthenticationPrincipal CustomUserDetails user
        ) {
                return ApiResponse.success(
                        "Booking retrieved",
                        service.get(bookingId, user)
                );
        }

        @PreAuthorize("hasRole('MERCHANT')")
        @SecurityRequirement(name = "BearerAuth")
        @GetMapping("/conversions")
        @Operation(
                summary = "List conversions belonging to the current merchant",
                description = """
                        Conversions are attributed bookings with a tracking link.
                        PENDING maps to booking status PENDING.
                        APPROVED maps to CONFIRMED or COMPLETED.
                        REJECTED maps to CANCELLED.
                        REVERSED maps to REFUNDED.
                        """
                )
                public ApiResponse<MerchantBookingDtos.ConversionPageResponse> listConversions(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @Parameter(
                                description = "PENDING, APPROVED, REJECTED, REVERSED, or ALL"
                        )
                        @RequestParam(defaultValue = "PENDING") String status,
                        @AuthenticationPrincipal CustomUserDetails user
                ) {
                return ApiResponse.success(
                        "Conversions retrieved",
                        service.listConversions(page, size, status, user)
                );
        }
        
        @PreAuthorize("hasRole('MERCHANT')")
        @SecurityRequirement(name = "BearerAuth")
        @PatchMapping("/{bookingId}/conversion-review")
        @Operation(
                summary = "Approve or reject a pending conversion",
                description = """
                        Approval changes booking_status from PENDING to CONFIRMED.
                        Rejection changes booking_status from PENDING to CANCELLED.
                        """
                )
                public ApiResponse<MerchantBookingDtos.ConversionReviewResponse>
                reviewConversion(
                        @PathVariable UUID bookingId,
                        @Valid @RequestBody
                        MerchantBookingDtos.ConversionReviewRequest request,
                        @AuthenticationPrincipal CustomUserDetails user
                ) {
                MerchantBookingDtos.ConversionReviewResponse result =
                        service.reviewConversion(bookingId, request, user);

                String message;

                if ("APPROVED".equals(result.conversionStatus())) {
                        message = result.commissionVerified()
                                ? "Conversion approved; commission was verified successfully"
                                : "Conversion approval failed because commission was not verified";
                } else if ("REJECTED".equals(result.conversionStatus())) {
                        message = "Conversion rejected; no commission was created";
                } else {
                        message = "Conversion reversed; commission reversal processed";
                }

                return ApiResponse.success(message, result);
        }
        
        @PostMapping(
                value = "/webhook",
                consumes = "application/json"
                )
                @PreAuthorize("permitAll()")
                @Operation(
                        summary = "Receive booking payment event",
                        description = """
                                Receives a payment event from the merchant checkout.
                                Authenticate with HMAC headers; a merchant JWT is not required.
                                """
                )
                public ApiResponse<MerchantBookingWebhookDtos.Result> receiveBookingEvent(
                        @RequestHeader("X-Merchant-Code") String merchantCode,
                        @RequestHeader("X-Webhook-Timestamp") String timestamp,
                        @RequestHeader("X-Webhook-Signature") String signature,
                        @RequestBody String rawBody
                ) {
                return ApiResponse.success(
                        "Webhook processed",
                        webhookService.process(
                                merchantCode,
                                timestamp,
                                signature,
                                rawBody
                        )
                );
        }

        @GetMapping("/webhook-credential")
        @PreAuthorize("hasRole('MERCHANT')")
        @SecurityRequirement(name = "BearerAuth")
        @Operation(
                summary = "Get this merchant's webhook credential",
                description = """
                        Returns the signing credential for the authenticated merchant.
                        Keep the signing secret in the merchant checkout backend.
                        Do not expose it in browser code.
                        """
        )
        public ApiResponse<MerchantBookingWebhookDtos.Credential>
        getWebhookCredential(
                @AuthenticationPrincipal CustomUserDetails user
        ) {
        return ApiResponse.success(
                "Webhook credential retrieved",
                webhookService.getCredential(user)
        );
        }

}