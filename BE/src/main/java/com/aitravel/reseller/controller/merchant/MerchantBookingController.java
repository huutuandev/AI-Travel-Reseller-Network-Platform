package com.aitravel.reseller.controller.merchant;

import com.aitravel.reseller.dto.respone.ApiResponse;
import com.aitravel.reseller.dto.respone.merchant.MerchantBookingDtos;
import com.aitravel.reseller.security.user.CustomUserDetails;
import com.aitravel.reseller.service.merchant.MerchantBookingService;
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
@PreAuthorize("hasRole('MERCHANT')")
@Tag(name = "Merchant bookings")
@SecurityRequirement(name = "BearerAuth")
public class MerchantBookingController {
    private final MerchantBookingService service;

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
        return ApiResponse.success(
                "Conversion reviewed",
                service.reviewConversion(bookingId, request, user)
        );
        }
}