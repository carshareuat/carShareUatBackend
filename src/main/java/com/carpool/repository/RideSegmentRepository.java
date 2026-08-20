package com.carpool.repository;

import com.carpool.entity.RideSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideSegmentRepository extends JpaRepository<RideSegment, UUID> {

    /**
     * Find all segments for a specific ride.
     */
    List<RideSegment> findByRideId(UUID rideId);

    /**
     * Find all segments between two specific stops.
     */
    @Query("SELECT rs FROM RideSegment rs WHERE rs.ride.id = :rideId AND rs.fromStop.id = :fromStopId AND rs.toStop.id = :toStopId ORDER BY rs.segmentOrder ASC")
    List<RideSegment> findByRideAndStops(UUID rideId, UUID fromStopId, UUID toStopId);

    /**
     * Find all segments where a location is the starting point.
     */
    @Query("SELECT rs FROM RideSegment rs WHERE rs.ride.id = :rideId AND rs.fromStop.id = :stopId")
    List<RideSegment> findByRideAndFromStop(UUID rideId, UUID stopId);

    /**
     * Find all segments where a location is the ending point.
     */
    @Query("SELECT rs FROM RideSegment rs WHERE rs.ride.id = :rideId AND rs.toStop.id = :stopId")
    List<RideSegment> findByRideAndToStop(UUID rideId, UUID stopId);

    /**
     * Find all segments for a ride with minimum available seats.
     */
    @Query("SELECT rs FROM RideSegment rs WHERE rs.ride.id = :rideId AND rs.availableSeats >= :minSeats")
    List<RideSegment> findByRideIdAndMinAvailableSeats(UUID rideId, int minSeats);

    /**
     * Find segments for a journey segment (from fromStopOrder to toStopOrder).
     * Returns all segments that start at fromOrder and end at toOrder.
     */
    @Query("SELECT rs FROM RideSegment rs " +
            "WHERE rs.ride.id = :rideId " +
            "AND rs.fromStop.stopOrder = :fromStopOrder " +
            "AND rs.toStop.stopOrder = :toStopOrder")
    Optional<RideSegment> findDirectSegment(UUID rideId, int fromStopOrder, int toStopOrder);

    /**
     * Find all segments that overlap with a given journey segment.
     * A segment overlaps if it starts before toStop and ends after fromStop.
     */
    @Query("SELECT rs FROM RideSegment rs " +
            "WHERE rs.ride.id = :rideId " +
            "AND rs.fromStop.stopOrder < :toStopOrder " +
            "AND rs.toStop.stopOrder > :fromStopOrder " +
            "ORDER BY rs.fromStop.stopOrder, rs.toStop.stopOrder")
    List<RideSegment> findOverlappingSegments(UUID rideId, int fromStopOrder, int toStopOrder);
}
