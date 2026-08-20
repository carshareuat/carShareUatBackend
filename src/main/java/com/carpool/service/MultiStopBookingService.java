package com.carpool.service;

import com.carpool.entity.Booking;
import com.carpool.entity.BookingStatus;
import com.carpool.entity.Ride;
import com.carpool.entity.RideSegment;
import com.carpool.entity.RideSegmentBooking;
import com.carpool.entity.RideStop;
import com.carpool.entity.User;
import com.carpool.exception.AppException;
import com.carpool.repository.BookingRepository;
import com.carpool.repository.RideRepository;
import com.carpool.repository.RideSegmentBookingRepository;
import com.carpool.repository.RideSegmentRepository;
import com.carpool.repository.RideStopRepository;
import com.carpool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing ride bookings with segment-level seat availability.
 * 
 * Responsibilities:
 * - Book segments of a multi-stop ride
 * - Manage seat occupancy per segment
 * - Calculate available seats across segments
 * - Handle booking cancellation and seat release
 * - Prevent overbooking with transactional locks
 */
@Service
@RequiredArgsConstructor
public class MultiStopBookingService {

    private static final Logger log = LoggerFactory.getLogger(MultiStopBookingService.class);

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final RideStopRepository rideStopRepository;
    private final RideSegmentRepository rideSegmentRepository;
    private final RideSegmentBookingRepository rideSegmentBookingRepository;
    private final UserRepository userRepository;

    // ========== MULTI-STOP SEGMENT BOOKING ==========

    /**
     * Book a segment of a multi-stop ride.
     * 
     * For a ride: Pondicherry → Villupuram → Salem → Erode → Coimbatore
     * A passenger can book: Salem → Erode (2 seats)
     * 
     * This will:
     * 1. Verify the ride exists and is active
     * 2. Find the segment from Salem to Erode
     * 3. Check all intermediate segments have sufficient seats
     * 4. Create booking and segment booking records
     * 5. Update segment seat availability
     */
    @Transactional
    public Booking bookSegment(UUID rideId, UUID passengerId, String passengerMobile,
                              String fromLocationName, String toLocationName, int seats) {
        log.debug("Booking segment for passenger {} from {} to {}", passengerId, fromLocationName, toLocationName);

        if (rideId == null || passengerId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_BOOKING", "Ride and passenger are required");
        }
        if (seats < 1) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_SEATS", "At least one seat is required");
        }
        if (passengerMobile == null || passengerMobile.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PASSENGER", "Passenger mobile is required");
        }
        if (fromLocationName == null || fromLocationName.isBlank() || toLocationName == null || toLocationName.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_ROUTE", "From and to locations are required");
        }

        // Fetch ride with lock to prevent concurrent issues
        Ride ride = rideRepository.findByIdForUpdate(rideId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ride not found"));

        // Verify ride is active
        if (ride.getStatus().name().equals("CANCELLED")) {
            throw new AppException(HttpStatus.CONFLICT, "RIDE_CANCELLED", "Ride is cancelled");
        }

        // Find from and to stops
        List<RideStop> fromStops = rideStopRepository.findByRideIdAndLocationName(rideId, fromLocationName);
        if (fromStops.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "From location not found in ride");
        }
        RideStop fromStop = fromStops.get(0);

        List<RideStop> toStops = rideStopRepository.findByRideIdAndLocationName(rideId, toLocationName);
        if (toStops.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "To location not found in ride");
        }
        RideStop toStop = toStops.get(0);

        // Validate stop order
        if (fromStop.getStopOrder() >= toStop.getStopOrder()) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ROUTE", "From location must come before to location");
        }

        // Check seat availability across all overlapping segments
        boolean seatsAvailable = checkAndReserveSeats(rideId, fromStop.getStopOrder(), toStop.getStopOrder(), seats);
        if (!seatsAvailable) {
            throw new AppException(HttpStatus.CONFLICT, "INSUFFICIENT_SEATS", "Insufficient seats available for the selected route");
        }

        // Get passenger user
        User passenger = userRepository.findById(passengerId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Passenger not found"));

        // Create booking
        Booking booking = new Booking();
        booking.setRide(ride);
        booking.setPassenger(passenger);
        booking.setPassengerMobile(passengerMobile);
        booking.setSeats(seats);
        booking.setStatus(BookingStatus.PENDING);
        booking.setFromStop(fromStop);
        booking.setToStop(toStop);
        booking.setNeedsRating(false);
        booking.setRated(false);

        // Get segment price
        List<RideSegment> matchingSegments = rideSegmentRepository.findByRideAndStops(rideId, fromStop.getId(), toStop.getId());
        if (!matchingSegments.isEmpty()) {
            booking.setSegmentPrice(matchingSegments.get(0).getPrice());
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Create segment booking records for all overlapping segments
        createSegmentBookings(savedBooking, rideId, fromStop.getStopOrder(), toStop.getStopOrder(), seats);

        log.debug("Booking created with ID: {} for {} seats", savedBooking.getId(), seats);
        return savedBooking;
    }

    /**
     * Check and reserve seats across all segments.
     * Decreases available_seats in all overlapping segments.
     */
    private boolean checkAndReserveSeats(UUID rideId, int fromStopOrder, int toStopOrder, int seats) {
        // Find all segments that overlap with the requested journey
        List<RideSegment> overlappingSegments = rideSegmentRepository.findOverlappingSegments(rideId, fromStopOrder, toStopOrder);

        if (overlappingSegments.isEmpty()) {
            return false;
        }

        // Check if all segments have sufficient seats
        for (RideSegment segment : overlappingSegments) {
            if (segment.getAvailableSeats() < seats) {
                return false;
            }
        }

        // All checks passed, reserve seats
        for (RideSegment segment : overlappingSegments) {
            segment.setAvailableSeats(segment.getAvailableSeats() - seats);
            rideSegmentRepository.save(segment);
        }

        // Update ride's overall available seats (take minimum from all segments)
        int minAvailableSeats = overlappingSegments.stream()
            .mapToInt(RideSegment::getAvailableSeats)
            .min()
            .orElse(0);

        Ride ride = rideRepository.findById(rideId).orElseThrow();
        ride.setAvailableSeats(Math.max(0, minAvailableSeats));
        rideRepository.save(ride);

        return true;
    }

    /**
     * Create RideSegmentBooking records for all overlapping segments.
     */
    private void createSegmentBookings(Booking booking, UUID rideId, int fromStopOrder, int toStopOrder, int seats) {
        Ride ride = rideRepository.findById(rideId).orElseThrow();
        List<RideStop> allStops = rideStopRepository.findByRideIdOrderByStopOrder(rideId);

        // Create a segment booking for each overlapping segment
        for (RideStop fromStop : allStops) {
            if (fromStop.getStopOrder() >= toStopOrder) break;

            for (RideStop toStop : allStops) {
                if (toStop.getStopOrder() <= fromStop.getStopOrder()) continue;
                if (toStop.getStopOrder() > toStopOrder) break;

                // Check if this segment overlaps with the booking range
                if (fromStop.getStopOrder() < toStopOrder && toStop.getStopOrder() > fromStopOrder) {
                    RideSegmentBooking segmentBooking = RideSegmentBooking.builder()
                        .ride(ride)
                        .booking(booking)
                        .fromStop(fromStop)
                        .toStop(toStop)
                        .seatCount(seats)
                        .build();

                    rideSegmentBookingRepository.save(segmentBooking);
                }
            }
        }
    }

    // ========== BOOKING CANCELLATION & SEAT RELEASE ==========

    /**
     * Cancel a booking and release reserved seats.
     */
    @Transactional
    public void cancelBooking(UUID bookingId, String cancellationReason) {
        log.debug("Cancelling booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new AppException(HttpStatus.CONFLICT, "ALREADY_CANCELLED", "Booking is already cancelled");
        }

        // Release seats for all segment bookings
        List<RideSegmentBooking> segmentBookings = rideSegmentBookingRepository.findByBookingId(bookingId);
        releaseSeatsFromBooking(booking, segmentBookings);

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(cancellationReason);
        booking.setCancelledAt(Instant.now());
        bookingRepository.save(booking);

        log.debug("Booking {} cancelled, seats released", bookingId);
    }

    /**
     * Release seats from a cancelled booking.
     */
    private void releaseSeatsFromBooking(Booking booking, List<RideSegmentBooking> segmentBookings) {
        // Collect unique segments
        List<UUID> segmentIds = new ArrayList<>();
        for (RideSegmentBooking sb : segmentBookings) {
            UUID fromStopId = sb.getFromStop().getId();
            UUID toStopId = sb.getToStop().getId();

            List<RideSegment> segments = rideSegmentRepository.findByRideAndStops(
                booking.getRide().getId(), fromStopId, toStopId);

            for (RideSegment segment : segments) {
                if (!segmentIds.contains(segment.getId())) {
                    segmentIds.add(segment.getId());
                }
            }
        }

        // Release seats from each segment
        for (UUID segmentId : segmentIds) {
            RideSegment segment = rideSegmentRepository.findById(segmentId).orElseThrow();
            int newAvailable = Math.min(segment.getTotalSeats(), 
                segment.getAvailableSeats() + booking.getSeats());
            segment.setAvailableSeats(newAvailable);
            rideSegmentRepository.save(segment);
        }

        // Update ride's available seats
        Ride ride = booking.getRide();
        int maxAvailableSeats = rideSegmentRepository.findByRideId(ride.getId()).stream()
            .mapToInt(RideSegment::getAvailableSeats)
            .min()
            .orElse(ride.getTotalSeats());
        ride.setAvailableSeats(maxAvailableSeats);
        rideRepository.save(ride);

        // Delete segment bookings
        rideSegmentBookingRepository.deleteAll(segmentBookings);
    }

    // ========== AVAILABILITY QUERIES ==========

    /**
     * Get available seats for a specific segment.
     */
    @Transactional(readOnly = true)
    public int getSegmentAvailability(UUID rideId, UUID fromStopId, UUID toStopId) {
        List<RideSegment> segments = rideSegmentRepository.findByRideAndStops(rideId, fromStopId, toStopId);
        if (segments.isEmpty()) {
            return 0;
        }
        return segments.get(0).getAvailableSeats();
    }

    /**
     * Get available seats for a journey (considering all overlapping segments).
     */
    @Transactional(readOnly = true)
    public int getJourneyAvailability(UUID rideId, int fromStopOrder, int toStopOrder) {
        List<RideSegment> overlappingSegments = rideSegmentRepository.findOverlappingSegments(
            rideId, fromStopOrder, toStopOrder);

        if (overlappingSegments.isEmpty()) {
            return 0;
        }

        // Return minimum available seats across all segments
        return overlappingSegments.stream()
            .mapToInt(RideSegment::getAvailableSeats)
            .min()
            .orElse(0);
    }

    /**
     * Get occupancy information for a ride segment.
     */
    @Transactional(readOnly = true)
    public SegmentOccupancy getSegmentOccupancy(UUID rideId, UUID fromStopId, UUID toStopId) {
        List<RideSegment> segments = rideSegmentRepository.findByRideAndStops(rideId, fromStopId, toStopId);
        if (segments.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Segment not found");
        }

        RideSegment segment = segments.get(0);
        int occupiedSeats = segment.getTotalSeats() - segment.getAvailableSeats();

        return SegmentOccupancy.builder()
            .segmentId(segment.getId())
            .rideId(rideId)
            .fromStop(fromStopId)
            .toStop(toStopId)
            .totalSeats(segment.getTotalSeats())
            .occupiedSeats(occupiedSeats)
            .availableSeats(segment.getAvailableSeats())
            .occupancyPercentage((occupiedSeats * 100) / segment.getTotalSeats())
            .build();
    }

    // ========== BOOKING STATUS ==========

    /**
     * Get booking details.
     */
    @Transactional(readOnly = true)
    public BookingDetails getBookingDetails(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Booking not found"));

        return BookingDetails.builder()
            .bookingId(booking.getId())
            .rideId(booking.getRide().getId())
            .passengerId(booking.getPassenger().getId())
            .fromLocation(booking.getFromStop() != null ? booking.getFromStop().getLocationName() : null)
            .toLocation(booking.getToStop() != null ? booking.getToStop().getLocationName() : null)
            .seats(booking.getSeats())
            .price(booking.getSegmentPrice())
            .status(booking.getStatus())
            .createdAt(booking.getCreatedAt())
            .build();
    }

    // ========== DTOs ==========

    public static class SegmentOccupancy {
        public UUID segmentId;
        public UUID rideId;
        public UUID fromStop;
        public UUID toStop;
        public int totalSeats;
        public int occupiedSeats;
        public int availableSeats;
        public int occupancyPercentage;

        public static SegmentOccupancyBuilder builder() {
            return new SegmentOccupancyBuilder();
        }

        public static class SegmentOccupancyBuilder {
            private UUID segmentId;
            private UUID rideId;
            private UUID fromStop;
            private UUID toStop;
            private int totalSeats;
            private int occupiedSeats;
            private int availableSeats;
            private int occupancyPercentage;

            public SegmentOccupancyBuilder segmentId(UUID segmentId) {
                this.segmentId = segmentId;
                return this;
            }

            public SegmentOccupancyBuilder rideId(UUID rideId) {
                this.rideId = rideId;
                return this;
            }

            public SegmentOccupancyBuilder fromStop(UUID fromStop) {
                this.fromStop = fromStop;
                return this;
            }

            public SegmentOccupancyBuilder toStop(UUID toStop) {
                this.toStop = toStop;
                return this;
            }

            public SegmentOccupancyBuilder totalSeats(int totalSeats) {
                this.totalSeats = totalSeats;
                return this;
            }

            public SegmentOccupancyBuilder occupiedSeats(int occupiedSeats) {
                this.occupiedSeats = occupiedSeats;
                return this;
            }

            public SegmentOccupancyBuilder availableSeats(int availableSeats) {
                this.availableSeats = availableSeats;
                return this;
            }

            public SegmentOccupancyBuilder occupancyPercentage(int occupancyPercentage) {
                this.occupancyPercentage = occupancyPercentage;
                return this;
            }

            public SegmentOccupancy build() {
                SegmentOccupancy occupancy = new SegmentOccupancy();
                occupancy.segmentId = this.segmentId;
                occupancy.rideId = this.rideId;
                occupancy.fromStop = this.fromStop;
                occupancy.toStop = this.toStop;
                occupancy.totalSeats = this.totalSeats;
                occupancy.occupiedSeats = this.occupiedSeats;
                occupancy.availableSeats = this.availableSeats;
                occupancy.occupancyPercentage = this.occupancyPercentage;
                return occupancy;
            }
        }
    }

    public static class BookingDetails {
        public UUID bookingId;
        public UUID rideId;
        public UUID passengerId;
        public String fromLocation;
        public String toLocation;
        public int seats;
        public java.math.BigDecimal price;
        public BookingStatus status;
        public Instant createdAt;

        public static BookingDetailsBuilder builder() {
            return new BookingDetailsBuilder();
        }

        public static class BookingDetailsBuilder {
            private UUID bookingId;
            private UUID rideId;
            private UUID passengerId;
            private String fromLocation;
            private String toLocation;
            private int seats;
            private java.math.BigDecimal price;
            private BookingStatus status;
            private Instant createdAt;

            public BookingDetailsBuilder bookingId(UUID bookingId) {
                this.bookingId = bookingId;
                return this;
            }

            public BookingDetailsBuilder rideId(UUID rideId) {
                this.rideId = rideId;
                return this;
            }

            public BookingDetailsBuilder passengerId(UUID passengerId) {
                this.passengerId = passengerId;
                return this;
            }

            public BookingDetailsBuilder fromLocation(String fromLocation) {
                this.fromLocation = fromLocation;
                return this;
            }

            public BookingDetailsBuilder toLocation(String toLocation) {
                this.toLocation = toLocation;
                return this;
            }

            public BookingDetailsBuilder seats(int seats) {
                this.seats = seats;
                return this;
            }

            public BookingDetailsBuilder price(java.math.BigDecimal price) {
                this.price = price;
                return this;
            }

            public BookingDetailsBuilder status(BookingStatus status) {
                this.status = status;
                return this;
            }

            public BookingDetailsBuilder createdAt(Instant createdAt) {
                this.createdAt = createdAt;
                return this;
            }

            public BookingDetails build() {
                BookingDetails details = new BookingDetails();
                details.bookingId = this.bookingId;
                details.rideId = this.rideId;
                details.passengerId = this.passengerId;
                details.fromLocation = this.fromLocation;
                details.toLocation = this.toLocation;
                details.seats = this.seats;
                details.price = this.price;
                details.status = this.status;
                details.createdAt = this.createdAt;
                return details;
            }
        }
    }
}
