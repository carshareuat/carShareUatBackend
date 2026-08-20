package com.carpool.service;

import com.carpool.dto.PageMeta;
import com.carpool.dto.ride.RideCreateRequest;
import com.carpool.dto.ride.RideResponse;
import com.carpool.entity.CancelledBy;
import com.carpool.entity.OwnerProfile;
import com.carpool.entity.Ride;
import com.carpool.entity.RideStatus;
import com.carpool.entity.Role;
import com.carpool.exception.AppException;
import com.carpool.mapper.RideMapper;
import com.carpool.repository.BookingRepository;
import com.carpool.repository.OwnerProfileRepository;
import com.carpool.repository.RideRepository;
import com.carpool.repository.RideSegmentRepository;
import com.carpool.repository.SubscriptionRepository;
import com.carpool.entity.SubscriptionStatus;
import com.carpool.security.AppUserPrincipal;
import com.carpool.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rideRepository;
    private final RideSegmentRepository rideSegmentRepository;
    private final com.carpool.repository.UserRepository userRepository;
    private final OwnerProfileRepository ownerRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final RideMapper rideMapper;
    private final AuthFacade authFacade;
    private final AuditService auditService;
    private final com.carpool.service.OwnerLocationService ownerLocationService;
    private final PassengerLocationService passengerLocationService;
    private final NotificationService notificationService;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public RideResponse create(RideCreateRequest request) {
        log.debug("RideService.create called with request={} by thread={}", request, Thread.currentThread().getName());
        AppUserPrincipal principal = authFacade.currentUser();
        OwnerProfile owner = ownerRepository.findByUserId(principal.getUserId())
            .orElseThrow(() -> new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Owner profile required"));
        if (!owner.isVerified() || !subscriptionRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream().anyMatch(s -> s.getStatus() == SubscriptionStatus.PAID)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin-approved subscription and owner verification are required to create rides");
        }
        if (rideRepository.existsByOwnerIdAndStatusNotIn(owner.getId(), List.of(RideStatus.COMPLETED, RideStatus.CANCELLED))) {
            throw new AppException(HttpStatus.CONFLICT, "ACTIVE_RIDE_EXISTS", "Complete or cancel your active ride before posting another ride");
        }
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "SEMANTIC_ERROR", "Ride date cannot be in past");
        }
        Ride ride = new Ride();
        ride.setOwner(owner);
        ride.setFromLocation(request.getFromLocation());
        ride.setToLocation(request.getToLocation());
        ride.setDate(request.getDate());
        ride.setStartTime(request.getStartTime());
        ride.setEndTime(request.getEndTime());
        ride.setPrice(request.getPrice());
        ride.setCarModel(request.getCarModel());
        ride.setTotalSeats(request.getTotalSeats());
        ride.setAvailableSeats(request.getTotalSeats());
        ride.setFemaleOnly(request.getFemaleOnly() != null && request.getFemaleOnly());
        ride.setStatus(RideStatus.ACTIVE);
        log.debug("About to save ride for owner={}", owner.getId());
        Ride saved = rideRepository.save(ride);
        log.debug("Ride saved id={}", saved.getId());
        // Confirm to the owner that their ride was posted successfully
        try {
            notificationService.create(principal.getUserId(), com.carpool.entity.NotificationType.RIDE_CREATED,
                "Ride posted", "Your ride " + saved.getFromLocation() + " → " + saved.getToLocation() + " was posted successfully.");
        } catch (Exception ignored) {}

        triggerPassengerRideBroadcast(saved.getId(), saved.getFromLocation(), saved.getToLocation());
        return rideMapper.toResponse(saved);
    }

    @Async
    public void triggerPassengerRideBroadcast(UUID rideId, String fromLocation, String toLocation) {
        try {
            String message = "New ride posted: " + fromLocation + " → " + toLocation;
            userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.carpool.entity.Role.PASSENGER)
                .forEach(u -> {
                    try {
                        notificationService.create(u.getId(), com.carpool.entity.NotificationType.RIDE_POSTED, "New ride posted", message);
                    } catch (Exception ignored) {
                        log.debug("Failed to broadcast ride {} notification to user {}", rideId, u.getId(), ignored);
                    }
                });
        } catch (Exception e) {
            log.warn("Passenger ride broadcast failed for ride {}: {}", rideId, e.getMessage());
        }
    }

    public Map<String, Object> list(String from, String to, LocalDate date, Integer passengers, RideStatus status, int page, int size, String sort) {
        RideStatus effective = status == null ? RideStatus.ACTIVE : status;
        Page<Ride> rides = rideRepository.findByStatusAndDateGreaterThanEqual(effective, LocalDate.now(),
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, sort == null || sort.isBlank() ? "date" : sort)));

        // determine requesting passenger gender (if any) and whether to hide owner's own rides
        String requesterGender = resolveRequesterGender();
        AppUserPrincipal principal = null;
        try { principal = authFacade.currentUser(); } catch (Exception ignored) {}
        boolean hideOwnerRidesForRequester = principal != null && principal.getRole() == Role.PASSENGER;
        java.util.UUID requesterUserId = principal == null ? null : principal.getUserId();
        java.util.UUID requesterOwnerId = principal == null ? null : principal.getOwnerId();
        String requesterMobile = principal == null ? null : principal.getMobile();

        List<RideResponse> filtered = rides.getContent().stream()
            .filter(r -> date == null || r.getDate().equals(date))
            .filter(r -> from == null || r.getFromLocation().toLowerCase().contains(from.toLowerCase()))
            .filter(r -> to == null || r.getToLocation().toLowerCase().contains(to.toLowerCase()))
            // Keep fully booked rides in results; frontend can mark them as FULL using availableSeats.
            .filter(r -> passengers == null || r.getTotalSeats() >= passengers)
            .filter(r -> r.getOwner().isVerified())
            .filter(r -> !r.isFemaleOnly() || requesterGender == null || "female".equalsIgnoreCase(requesterGender))
            .filter(r -> {
                if (!hideOwnerRidesForRequester) return true;
                try {
                    if (requesterOwnerId != null && requesterOwnerId.equals(r.getOwner().getId())) return false;
                    if (requesterUserId != null && r.getOwner().getUser() != null && requesterUserId.equals(r.getOwner().getUser().getId())) return false;
                    if (requesterMobile != null && r.getOwner().getMobile() != null && requesterMobile.equals(r.getOwner().getMobile())) return false;
                } catch (Exception ignored) {}
                return true;
            })
            .map(rideMapper::toResponse)
            .toList();

        return Map.of(
            "items", filtered,
            "meta", PageMeta.builder().page(page).size(size).totalElements(rides.getTotalElements()).totalPages(rides.getTotalPages()).build()
        );
    }

    public RideResponse get(UUID rideId) {
        return rideMapper.toResponse(findRide(rideId));
    }

    @Transactional
    public RideResponse update(UUID rideId, RideStatus status, String cancellationReason, String cancellationNote) {
        Ride ride = findRide(rideId);
        assertOwnerOrAdmin(ride);
        if (status == null || status == ride.getStatus()) {
            return rideMapper.toResponse(ride);
        }
        if (ride.getStatus() != RideStatus.ACTIVE || (status != RideStatus.COMPLETED && status != RideStatus.CANCELLED)) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Invalid status transition");
        }

        if (status == RideStatus.CANCELLED && (cancellationReason == null || cancellationReason.isBlank())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Cancellation reason is required");
        }

        if (status == RideStatus.COMPLETED) {
            ride.setStatus(RideStatus.COMPLETED);
            bookingService.cascadeForRideCompletion(ride);
        } else {
            ride.setStatus(RideStatus.CANCELLED);
            ride.setCancellationReason(cancellationReason.trim());
            ride.setCancellationNote(cancellationNote);
            ride.setCancelledAt(Instant.now());
            bookingService.cascadeForRideCancellation(ride, ride.getCancellationReason(), cancellationNote, CancelledBy.OWNER);
        }
        Ride saved = rideRepository.save(ride);
        auditService.log("RIDE_STATUS_CHANGE", authFacade.currentUser().getUserId().toString(), ride.getId().toString(), "{\"status\":\"" + saved.getStatus() + "\"}");
        return rideMapper.toResponse(saved);
    }

    @Transactional
    public RideResponse delete(UUID rideId) {
        Ride ride = findRide(rideId);
        assertOwnerOrAdmin(ride);
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason("DELETED_BY_OWNER");
        ride.setCancelledAt(Instant.now());
        return rideMapper.toResponse(rideRepository.save(ride));
    }

    public Ride findRide(UUID id) {
        return rideRepository.findById(id).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ride not found"));
    }

    @Transactional(readOnly = true)
    public List<RideResponse> byOwner(UUID ownerId) {
        return rideRepository.findByOwnerId(ownerId).stream().map(ride -> {
            RideResponse response = rideMapper.toResponse(ride);

            if (ride.isMultiStop()) {
                rideSegmentRepository.findDirectSegment(ride.getId(), 0, ride.getTotalStops() - 1)
                    .map(segment -> {
                        response.setPrice(segment.getPrice());
                        return segment;
                    });
            }

            return response;
        }).toList();
    }

    /**
     * Return a simple simulated location for the ride's owner. This is a deterministic
     * moving coordinate based on the ride id and time so frontend can demonstrate live tracking.
     */
    public java.util.Map<String, Double> getLocation(UUID rideId) {
        Ride ride = findRide(rideId);
        // prefer real owner-reported location if available
        UUID ownerId = ride.getOwner().getId();
        return ownerLocationService.findByOwnerId(ownerId).map(ownerLocationService::toMap)
            .orElseGet(() -> {
                // fallback to deterministic simulated location
                String idStr = ride.getId().toString();
                int h = Math.abs(idStr.hashCode());
                double baseLat = 13.0 + (h % 1000) / 1000.0; // around ~13-14 degrees
                double baseLon = 80.0 + ((h / 1000) % 1000) / 1000.0; // around ~80-81 degrees
                long t = System.currentTimeMillis() / 3000L;
                double deltaLat = Math.sin(t / 4.0 + (h % 10)) / 100.0; // small oscillation
                double deltaLon = Math.cos(t / 5.0 + (h % 7)) / 100.0;
                double lat = baseLat + deltaLat;
                double lon = baseLon + deltaLon;
                return Map.of("lat", lat, "lon", lon);
            });
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Double> getPassengerLocation(UUID rideId) {
        Ride ride = findRide(rideId);
        assertOwnerOrAdmin(ride);
        return bookingRepository.findByRideIdAndStatusIn(rideId, List.of(com.carpool.entity.BookingStatus.ACCEPTED)).stream()
            .map(booking -> passengerLocationService.findByPassengerId(booking.getPassenger().getId())
                .map(passengerLocationService::toMap).orElse(null))
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    private String resolveRequesterGender() {
        try {
            AppUserPrincipal principal = authFacade.currentUser();
            if (principal.getRole() != Role.PASSENGER) {
                return null;
            }
            return userRepository.findById(principal.getUserId()).map(u -> u.getGender()).orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void assertOwnerOrAdmin(Ride ride) {
        AppUserPrincipal principal = authFacade.currentUser();
        if (principal.getRole() == Role.ADMIN) {
            return;
        }
        if (principal.getOwnerId() == null || !ride.getOwner().getId().equals(principal.getOwnerId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not your ride");
        }
    }
}