package com.carpool.service;

import com.carpool.dto.booking.BookingCancelRequest;
import com.carpool.dto.booking.BookingCreateRequest;
import com.carpool.dto.booking.BookingDecisionRequest;
import com.carpool.dto.booking.BookingResponse;
import com.carpool.entity.Booking;
import com.carpool.entity.BookingStatus;
import com.carpool.entity.CancelledBy;
import com.carpool.entity.NotificationType;
import com.carpool.entity.Ride;
import com.carpool.entity.RideStatus;
import com.carpool.entity.Role;
import com.carpool.exception.AppException;
import com.carpool.mapper.BookingMapper;
import com.carpool.repository.BookingRepository;
import com.carpool.repository.RideRepository;
import com.carpool.repository.UserRepository;
import com.carpool.security.AppUserPrincipal;
import com.carpool.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final AuthFacade authFacade;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public BookingResponse create(BookingCreateRequest request) {
        AppUserPrincipal principal = authFacade.currentUser();
        if (principal.getRole() != Role.PASSENGER) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only passenger can book");
        }
        if (bookingRepository.existsByPassengerIdAndStatusAndRatedFalse(principal.getUserId(), BookingStatus.COMPLETED)) {
            throw new AppException(HttpStatus.CONFLICT, "CONFLICT", "Rate your previous completed booking first");
        }
        Ride ride = rideRepository.findByIdForUpdate(request.getRideId())
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ride not found"));
        if (ride.getStatus() != RideStatus.ACTIVE || ride.getDate().isBefore(LocalDate.now())) {
            throw new AppException(HttpStatus.CONFLICT, "CONFLICT", "Ride unavailable");
        }
        if (request.getSeats() < 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Seats must be >= 1");
        }
        if (bookingRepository.hasActiveBooking(ride.getId(), principal.getUserId(), List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED))) {
            throw new AppException(HttpStatus.CONFLICT, "DUPLICATE_BOOKING", "Duplicate active booking");
        }
        if (ride.getOwner().getMobile() != null && principal.getMobile() != null
            && ride.getOwner().getMobile().equals(principal.getMobile())) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Cannot book your own ride");
        }
        if (request.getSeats() > ride.getAvailableSeats()) {
            throw new AppException(HttpStatus.CONFLICT, "OVERBOOKING", "Insufficient seats");
        }

        // Seat reservation happens at acceptance time, not at pending creation.
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(userRepository.findById(principal.getUserId()).orElseThrow());
        booking.setPassengerMobile(principal.getMobile());
        booking.setSeats(request.getSeats());
        booking.setStatus(BookingStatus.PENDING);
        Booking saved = bookingRepository.save(booking);

        // Notify owner about new booking request
        notificationService.create(ride.getOwner().getUser().getId(), NotificationType.NEW_BOOKING_REQUEST,
            "New booking request", "A passenger requested seats on your ride.");
        // Notify passenger that booking is pending
        notificationService.create(saved.getPassenger().getId(), NotificationType.BOOKING_PENDING,
            "Booking pending", "Your booking request is pending. The owner will review it shortly.");
        auditService.log("BOOKING_CREATE", principal.getUserId().toString(), saved.getId().toString(), "{\"rideId\":\"" + ride.getId() + "\"}");
        return bookingMapper.toResponse(saved);
    }

    public List<BookingResponse> myBookings() {
        UUID userId = authFacade.currentUser().getUserId();
        return bookingRepository.findByPassengerId(userId).stream().map(bookingMapper::toResponse).toList();
    }

    public BookingResponse get(UUID bookingId) {
        Booking b = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Booking not found"));
        AppUserPrincipal principal = authFacade.currentUser();
        boolean owner = principal.getOwnerId() != null && b.getRide().getOwner().getId().equals(principal.getOwnerId());
        boolean passenger = b.getPassenger().getId().equals(principal.getUserId());
        if (!(owner || passenger || principal.getRole() == Role.ADMIN)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden");
        }
        return bookingMapper.toResponse(b);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId, BookingCancelRequest request) {
        Booking b = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Booking not found"));
        AppUserPrincipal principal = authFacade.currentUser();
        if (!b.getPassenger().getId().equals(principal.getUserId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not your booking");
        }
        if (!(b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.ACCEPTED)) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Cannot cancel booking in current status");
        }

        if (b.getStatus() == BookingStatus.ACCEPTED) {
            Ride ride = rideRepository.findByIdForUpdate(b.getRide().getId()).orElseThrow();
            ride.setAvailableSeats(ride.getAvailableSeats() + b.getSeats());
            rideRepository.save(ride);
        }

        b.setStatus(BookingStatus.CANCELLED);
        b.setCancellationReason(request.getReason());
        b.setCancellationNote(request.getNote());
        b.setCancelledBy(CancelledBy.PASSENGER);
        b.setCancelledAt(Instant.now());
        bookingRepository.save(b);

        notificationService.create(b.getRide().getOwner().getUser().getId(), NotificationType.PASSENGER_CANCELLATION,
            "Passenger cancelled booking", "A passenger cancelled their booking.");
        notificationService.create(principal.getUserId(), NotificationType.PASSENGER_CANCELLATION,
            "Booking cancelled", "You cancelled your booking.");
        auditService.log("BOOKING_CANCEL", principal.getUserId().toString(), b.getId().toString(), "{}");
        return bookingMapper.toResponse(b);
    }

    public List<BookingResponse> rideBookings(UUID rideId) {
        AppUserPrincipal principal = authFacade.currentUser();
        Ride ride = rideRepository.findById(rideId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ride not found"));
        if (!(principal.getRole() == Role.ADMIN || (principal.getOwnerId() != null && principal.getOwnerId().equals(ride.getOwner().getId())))) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden");
        }
        return bookingRepository.findByRideId(rideId).stream().map(bookingMapper::toResponse).toList();
    }

    public List<BookingResponse> confirmedPassengers(UUID rideId) {
        rideRepository.findById(rideId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ride not found"));
        return bookingRepository.findByRideIdAndStatusIn(rideId, List.of(BookingStatus.ACCEPTED))
            .stream().map(booking -> {
                var response = bookingMapper.toResponse(booking);
                response.setPassengerMobile(null);
                return response;
            }).toList();
    }

    @Transactional
    public BookingResponse decide(UUID bookingId, BookingDecisionRequest request) {
        Booking b = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Booking not found"));
        AppUserPrincipal principal = authFacade.currentUser();
        if (principal.getOwnerId() == null || !principal.getOwnerId().equals(b.getRide().getOwner().getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not ride owner");
        }
        if (b.getStatus() != BookingStatus.PENDING) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Only pending booking can be decided");
        }
        if (request.getStatus() != BookingStatus.ACCEPTED && request.getStatus() != BookingStatus.REJECTED) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Status must be ACCEPTED or REJECTED");
        }
        if (request.getStatus() == BookingStatus.ACCEPTED) {
            Ride ride = rideRepository.findByIdForUpdate(b.getRide().getId()).orElseThrow();
            if (ride.getAvailableSeats() < b.getSeats()) {
                throw new AppException(HttpStatus.CONFLICT, "OVERBOOKING", "Insufficient seats");
            }
            ride.setAvailableSeats(ride.getAvailableSeats() - b.getSeats());
            rideRepository.save(ride);
            b.setStatus(BookingStatus.ACCEPTED);
            notificationService.create(b.getPassenger().getId(), NotificationType.BOOKING_ACCEPTED,
                "Booking accepted", "Your booking was accepted.");
        } else {
            b.setStatus(BookingStatus.REJECTED);
            notificationService.create(b.getPassenger().getId(), NotificationType.BOOKING_REJECTED,
                "Booking rejected", "Your booking was rejected.");
        }
        bookingRepository.save(b);
        auditService.log("BOOKING_DECISION", principal.getUserId().toString(), b.getId().toString(), "{\"status\":\"" + b.getStatus() + "\"}");
        return bookingMapper.toResponse(b);
    }

    @Transactional
    public void cascadeForRideCompletion(Ride ride) {
        List<Booking> accepted = bookingRepository.findByRideIdAndStatusIn(ride.getId(), List.of(BookingStatus.ACCEPTED));
        for (Booking b : accepted) {
            b.setStatus(BookingStatus.COMPLETED);
            b.setNeedsRating(true);
            notificationService.create(b.getPassenger().getId(), NotificationType.RATING_AVAILABLE,
                "Rate your ride", "You can now rate your completed ride.");
        }
        bookingRepository.saveAll(accepted);
    }

    @Transactional
    public void cascadeForRideCancellation(Ride ride, String reason, String note, CancelledBy cancelledBy) {
        List<Booking> affected = bookingRepository.findByRideIdAndStatusIn(ride.getId(), List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED));
        int restoreSeats = 0;
        for (Booking b : affected) {
            if (b.getStatus() == BookingStatus.ACCEPTED) {
                restoreSeats += b.getSeats();
            }
            b.setStatus(BookingStatus.CANCELLED);
            b.setCancellationReason(reason);
            b.setCancellationNote(note);
            b.setCancelledBy(cancelledBy);
            b.setCancelledAt(Instant.now());
            notificationService.create(b.getPassenger().getId(), NotificationType.OWNER_RIDE_CANCELLATION,
                "Ride cancelled", "The owner cancelled the ride.");
        }
        if (restoreSeats > 0) {
            Ride locked = rideRepository.findByIdForUpdate(ride.getId()).orElseThrow();
            locked.setAvailableSeats(Math.min(locked.getTotalSeats(), locked.getAvailableSeats() + restoreSeats));
            rideRepository.save(locked);
        }
        bookingRepository.saveAll(affected);
    }
}