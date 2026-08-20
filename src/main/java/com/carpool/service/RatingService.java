package com.carpool.service;

import com.carpool.dto.rating.RatingBreakdownResponse;
import com.carpool.dto.rating.RatingRequest;
import com.carpool.dto.rating.RatingResponse;
import com.carpool.entity.Booking;
import com.carpool.entity.BookingStatus;
import com.carpool.entity.OwnerProfile;
import com.carpool.entity.Rating;
import com.carpool.exception.AppException;
import com.carpool.repository.BookingRepository;
import com.carpool.repository.OwnerProfileRepository;
import com.carpool.repository.RatingRepository;
import com.carpool.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final BookingRepository bookingRepository;
    private final OwnerProfileRepository ownerRepository;
    private final AuthFacade authFacade;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public RatingResponse submit(UUID bookingId, RatingRequest request) {
        UUID passengerId = authFacade.currentUser().getUserId();
        Booking booking = bookingRepository.findByIdAndPassengerId(bookingId, passengerId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Booking not found"));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Booking is not completed");
        }
        if (booking.isRated()) {
            throw new AppException(HttpStatus.CONFLICT, "DUPLICATE_RATING", "Rating already submitted");
        }

        Rating rating = new Rating();
        rating.setBooking(booking);
        rating.setOwner(booking.getRide().getOwner());
        rating.setPassenger(booking.getPassenger());
        rating.setRating(request.getRating());
        rating.setNote(request.getNote());
        rating = ratingRepository.save(rating);

        booking.setRated(true);
        booking.setNeedsRating(false);
        bookingRepository.save(booking);

        recalcOwnerRating(booking.getRide().getOwner().getId());
        auditService.log("RATING_SUBMIT", passengerId.toString(), rating.getId().toString(), "{}");

        // notify owner about received rating
        try {
            var ownerUserId = booking.getRide().getOwner().getUser().getId();
            notificationService.create(ownerUserId, com.carpool.entity.NotificationType.RATING_RECEIVED,
                "New rating received", "You received a new rating for your ride.");
        } catch (Exception ignored) {}

        return toResponse(rating);
    }

    public RatingBreakdownResponse breakdown(UUID ownerId) {
        List<Rating> ratings = ratingRepository.findByOwnerId(ownerId);
        Map<Integer, Long> stars = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            stars.put(i, 0L);
        }
        int sum = 0;
        for (Rating r : ratings) {
            stars.computeIfPresent(r.getRating(), (k, v) -> v + 1);
            sum += r.getRating();
        }
        BigDecimal avg = ratings.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        return RatingBreakdownResponse.builder().average(avg).count(ratings.size()).stars(stars).build();
    }

    private void recalcOwnerRating(UUID ownerId) {
        OwnerProfile owner = ownerRepository.findById(ownerId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Owner not found"));
        List<Rating> ratings = ratingRepository.findByOwnerId(ownerId);
        int sum = ratings.stream().mapToInt(Rating::getRating).sum();
        BigDecimal avg = ratings.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(ratings.size()), 2, RoundingMode.HALF_UP);
        owner.setAverageRating(avg);
        owner.setRatingsCount(ratings.size());
        ownerRepository.save(owner);
    }

    private RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
            .id(rating.getId())
            .bookingId(rating.getBooking().getId())
            .ownerId(rating.getOwner().getId())
            .passengerId(rating.getPassenger().getId())
            .rating(rating.getRating())
            .note(rating.getNote())
            .createdAt(rating.getCreatedAt())
            .updatedAt(rating.getUpdatedAt())
            .build();
    }
}
