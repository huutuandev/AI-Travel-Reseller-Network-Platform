package com.aitravel.reseller.service.merchant;

import com.aitravel.reseller.dto.respone.merchant.MerchantBookingDtos;
import com.aitravel.reseller.entity.Merchant;
import com.aitravel.reseller.exception.ResourceNotFoundException;
import com.aitravel.reseller.repository.MerchantRepository;
import com.aitravel.reseller.repository.merchant.MerchantBookingQueryRepository;
import com.aitravel.reseller.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.List;

import com.aitravel.reseller.repository.merchant.MerchantCommissionPayoutRepository;

@Service
@RequiredArgsConstructor
public class MerchantBookingService {
    private static final Set<String> BOOKING_STATUSES = Set.of(
            "PENDING",
            "CONFIRMED",
            "CANCELLED",
            "COMPLETED",
            "REFUNDED"
    );

    private final MerchantBookingQueryRepository bookingRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantCommissionPayoutRepository financeRepository;

    @Transactional(readOnly = true)
    public MerchantBookingDtos.PageResponse list(
            int page,
            int size,
            String search,
            String status,
            UUID productId,
            UUID resellerId,
            Instant from,
            Instant to,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);

        String normalizedSearch = search == null ? "" : search.trim();
        if (normalizedSearch.length() > 200) {
            throw new IllegalArgumentException(
                    "search must not exceed 200 characters"
            );
        }

        String normalizedStatus = normalizeStatus(status);
        if (!normalizedStatus.isEmpty()
                && !BOOKING_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid booking status");
        }

        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "from must be earlier than to"
            );
        }

        long totalElements = bookingRepository.countForMerchant(
                merchant.getId(),
                normalizedSearch,
                normalizedStatus,
                productId,
                resellerId,
                from,
                to
        );

        var content = bookingRepository.findPageForMerchant(
                merchant.getId(),
                normalizedSearch,
                normalizedStatus,
                productId,
                resellerId,
                from,
                to,
                page,
                size
        );

        int totalPages = totalElements == 0
                ? 0
                : (int) ((totalElements + size - 1) / size);

        return new MerchantBookingDtos.PageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public MerchantBookingDtos.BookingItem get(
            UUID bookingId,
            CustomUserDetails user
    ) {
        Merchant merchant = requireActiveMerchant(user);

        return bookingRepository.findDetailForMerchant(
                        merchant.getId(),
                        bookingId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found"
                ));
    }

    private Merchant requireActiveMerchant(CustomUserDetails user) {
        if (user == null) {
            throw new AccessDeniedException("Merchant account is required");
        }

        Merchant merchant = merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException(
                        "This user is not linked to a merchant"
                ));

        if (!"ACTIVE".equals(merchant.getStatus())) {
            throw new AccessDeniedException(
                    "Merchant account is not active"
            );
        }

        return merchant;
    }

    private String normalizeStatus(String status) {
        return status == null
                ? ""
                : status.trim().toUpperCase(Locale.ROOT);
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be zero or greater"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100"
            );
        }
    }

        @Transactional(readOnly = true)
        public MerchantBookingDtos.ConversionPageResponse listConversions(
                int page,
                int size,
                String status,
                CustomUserDetails user
        ) {
        Merchant merchant = requireActiveMerchant(user);
        validatePage(page, size);

        String normalizedStatus = normalizeStatus(status);

        List<String> bookingStatuses = switch (normalizedStatus) {
                case "", "ALL" -> List.of(
                        "PENDING",
                        "CONFIRMED",
                        "COMPLETED",
                        "CANCELLED",
                        "REFUNDED"
                );
                case "PENDING" -> List.of("PENDING");
                case "APPROVED" -> List.of("CONFIRMED", "COMPLETED");
                case "REJECTED" -> List.of("CANCELLED");
                case "REVERSED" -> List.of("REFUNDED");
                default -> throw new IllegalArgumentException(
                        "Conversion status must be PENDING, APPROVED, REJECTED, or REVERSED"
                );
        };

        long totalElements = bookingRepository.countConversionsForMerchant(
                merchant.getId(),
                bookingStatuses
        );

        List<MerchantBookingDtos.ConversionItem> content =
                bookingRepository.findConversionPageForMerchant(
                                merchant.getId(),
                                bookingStatuses,
                                page,
                                size
                        )
                        .stream()
                        .map(booking -> new MerchantBookingDtos.ConversionItem(
                                booking,
                                toConversionStatus(booking.bookingStatus())
                        ))
                        .toList();

        int totalPages = totalElements == 0
                ? 0
                : (int) ((totalElements + size - 1) / size);

        return new MerchantBookingDtos.ConversionPageResponse(
                content,
                page,
                size,
                totalElements,
                totalPages
        );
        }

        @Transactional
        public MerchantBookingDtos.ConversionReviewResponse reviewConversion(
                        UUID bookingId,
                        MerchantBookingDtos.ConversionReviewRequest request,
                        CustomUserDetails user
                ) {
                Merchant merchant = requireActiveMerchant(user);

                if (request == null || request.decision() == null
                        || request.decision().isBlank()) {
                        throw new IllegalArgumentException("decision is required");
                }

                String decision = request.decision()
                        .trim()
                        .toUpperCase(Locale.ROOT);

                MerchantBookingDtos.BookingItem booking =
                        bookingRepository.findDetailForMerchant(
                                        merchant.getId(),
                                        bookingId
                                )
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Booking not found"
                                ));

                if (booking.trackingLinkId() == null) {
                        throw new IllegalStateException(
                                "This booking is not attributed to a reseller and cannot be reviewed as a conversion"
                        );
                }

                String targetBookingStatus;
                String conversionStatus;
                List<String> allowedCurrentStatuses;

                switch (decision) {
                        case "APPROVE" -> {
                        targetBookingStatus = "CONFIRMED";
                        conversionStatus = "APPROVED";
                        allowedCurrentStatuses = List.of("PENDING");
                        }
                        case "REJECT" -> {
                        targetBookingStatus = "CANCELLED";
                        conversionStatus = "REJECTED";
                        allowedCurrentStatuses = List.of("PENDING");
                        }
                        case "REVERSE" -> {
                        targetBookingStatus = "REFUNDED";
                        conversionStatus = "REVERSED";
                        allowedCurrentStatuses = List.of("CONFIRMED", "COMPLETED");
                        }
                        default -> throw new IllegalArgumentException(
                                "decision must be APPROVE, REJECT, or REVERSE"
                        );
                }

                if (!allowedCurrentStatuses.contains(booking.bookingStatus())) {
                        throw new IllegalStateException(
                                "Decision " + decision
                                        + " is not allowed for booking status "
                                        + booking.bookingStatus()
                        );
                }

                int updated = bookingRepository.transitionConversionBooking(
                        merchant.getId(),
                        bookingId,
                        allowedCurrentStatuses,
                        targetBookingStatus
                );

                if (updated == 0) {
                        throw new IllegalStateException(
                                "Booking status changed before the conversion decision was saved; reload and try again"
                        );
                }

                MerchantBookingDtos.BookingItem updatedBooking =
                        bookingRepository.findDetailForMerchant(
                                        merchant.getId(),
                                        bookingId
                                )
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Booking not found"
                                ));

                return new MerchantBookingDtos.ConversionReviewResponse(
                        bookingId,
                        conversionStatus,
                        updatedBooking.bookingStatus(),
                        updatedBooking.updatedAt()
                );
        }

        private String toConversionStatus(String bookingStatus) {
        return switch (bookingStatus) {
                case "PENDING" -> "PENDING";
                case "CONFIRMED", "COMPLETED" -> "APPROVED";
                case "CANCELLED" -> "REJECTED";
                case "REFUNDED" -> "REVERSED";
                default -> throw new IllegalArgumentException(
                        "Unsupported booking status: " + bookingStatus
                );
        };
        }
}