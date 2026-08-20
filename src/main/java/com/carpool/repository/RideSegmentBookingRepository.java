package com.carpool.repository;

import com.carpool.entity.RideSegmentBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RideSegmentBookingRepository extends JpaRepository<RideSegmentBooking, UUID> {

    /**
     * Find all segment bookings for a specific ride.
     */
    List<RideSegmentBooking> findByRideId(UUID rideId);

    /**
     * Find all segment bookings for a specific booking.
     */
    List<RideSegmentBooking> findByBookingId(UUID bookingId);

    /**
     * Find all segment bookings that overlap with a given segment.
     * Returns bookings where:
     * - They start before the given toStop
     * - They end after the given fromStop
     */
    @Query("SELECT sb FROM RideSegmentBooking sb " +
            "WHERE sb.ride.id = :rideId " +
            "AND sb.fromStop.stopOrder < :toStopOrder " +
            "AND sb.toStop.stopOrder > :fromStopOrder")
    List<RideSegmentBooking> findOverlappingBookings(UUID rideId, int fromStopOrder, int toStopOrder);

    /**
     * Calculate total seats occupied for a specific segment.
     */
    @Query("SELECT COALESCE(SUM(sb.seatCount), 0) FROM RideSegmentBooking sb " +
            "WHERE sb.ride.id = :rideId " +
            "AND sb.fromStop.id = :fromStopId " +
            "AND sb.toStop.id = :toStopId " +
            "AND sb.booking.status IN ('CONFIRMED', 'PENDING')")
    int calculateOccupiedSeats(UUID rideId, UUID fromStopId, UUID toStopId);

    /**
     * Find all segment bookings for overlapping segments on a journey segment.
     */
    @Query("SELECT sb FROM RideSegmentBooking sb " +
            "WHERE sb.ride.id = :rideId " +
            "AND sb.fromStop.stopOrder < :toStopOrder " +
            "AND sb.toStop.stopOrder > :fromStopOrder " +
            "AND sb.booking.status IN ('CONFIRMED', 'PENDING')")
    List<RideSegmentBooking> findActiveBookingsInRange(UUID rideId, int fromStopOrder, int toStopOrder);
}
