package com.carpool.repository;

import com.carpool.entity.RideStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideStopRepository extends JpaRepository<RideStop, UUID> {

    /**
     * Find all stops for a specific ride in order.
     */
    @Query("SELECT rs FROM RideStop rs WHERE rs.ride.id = :rideId ORDER BY rs.stopOrder ASC")
    List<RideStop> findByRideIdOrderByStopOrder(UUID rideId);

    /**
     * Find a specific stop by ride and stop order.
     */
    Optional<RideStop> findByRideIdAndStopOrder(UUID rideId, int stopOrder);

    /**
     * Find stops by location name for a specific ride.
     */
    @Query("SELECT rs FROM RideStop rs WHERE rs.ride.id = :rideId AND LOWER(TRIM(rs.locationName)) = LOWER(TRIM(:locationName)) ORDER BY rs.stopOrder ASC")
    List<RideStop> findByRideIdAndLocationName(UUID rideId, String locationName);

    /**
     * Check if a location exists in a ride.
     */
    @Query("SELECT COUNT(rs) > 0 FROM RideStop rs WHERE rs.ride.id = :rideId AND LOWER(rs.locationName) = LOWER(:locationName)")
    boolean existsByRideIdAndLocationName(UUID rideId, String locationName);

    /**
     * Find all stops for a ride between two stop orders (inclusive).
     */
    @Query("SELECT rs FROM RideStop rs WHERE rs.ride.id = :rideId AND rs.stopOrder >= :fromOrder AND rs.stopOrder <= :toOrder ORDER BY rs.stopOrder ASC")
    List<RideStop> findStopsBetweenOrders(UUID rideId, int fromOrder, int toOrder);
}
