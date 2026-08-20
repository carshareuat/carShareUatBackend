package com.carpool.repository;

import com.carpool.entity.Ride;
import com.carpool.entity.RideStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RideRepository extends JpaRepository<Ride, UUID> {
    Page<Ride> findByStatusAndDateGreaterThanEqual(RideStatus status, LocalDate date, Pageable pageable);
    List<Ride> findByOwnerId(UUID ownerId);

        boolean existsByOwnerIdAndStatusNotIn(UUID ownerId, List<RideStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Ride r where r.id = :rideId")
    Optional<Ride> findByIdForUpdate(UUID rideId);

    // ========== Multi-Stop Ride Queries ==========

    /**
     * Find all multi-stop rides that are active and not in the past.
     */
    @Query("SELECT r FROM Ride r WHERE r.isMultiStop = true AND r.status = 'ACTIVE' AND r.date >= :currentDate")
    Page<Ride> findActiveMultiStopRides(LocalDate currentDate, Pageable pageable);

    /**
     * Find rides that have a specific location as a stop.
     * Used to match passenger's from location.
     */
    @Query("SELECT DISTINCT r FROM Ride r " +
            "JOIN r.stops rs " +
            "WHERE r.status = 'ACTIVE' " +
            "AND r.isMultiStop = true " +
            "AND r.date >= :currentDate " +
            "AND LOWER(rs.locationName) = LOWER(:locationName)")
    Page<Ride> findRidesWithStop(String locationName, LocalDate currentDate, Pageable pageable);

    /**
     * Find rides where both from and to locations exist as stops.
     * Ensures from stop comes before to stop.
     */
    @Query("SELECT DISTINCT r FROM Ride r " +
            "JOIN r.stops fromStop ON LOWER(TRIM(fromStop.locationName)) = LOWER(TRIM(:fromLocation)) " +
            "JOIN r.stops toStop ON LOWER(TRIM(toStop.locationName)) = LOWER(TRIM(:toLocation)) " +
            "WHERE r.status = 'ACTIVE' " +
            "AND r.isMultiStop = true " +
            "AND r.date >= :currentDate " +
            "AND fromStop.stopOrder < toStop.stopOrder")
    Page<Ride> findRidesWithBothStops(String fromLocation, String toLocation, LocalDate currentDate, Pageable pageable);

    /**
     * Find rides with multi-stop capability for a specific date.
     */
    @Query("SELECT r FROM Ride r WHERE r.isMultiStop = true AND r.status = 'ACTIVE' AND r.date = :date ORDER BY r.createdAt DESC")
    List<Ride> findMultiStopRidesByDate(LocalDate date);

    /**
     * Find rides owned by a specific owner that are multi-stop.
     */
    @Query("SELECT r FROM Ride r WHERE r.owner.id = :ownerId AND r.isMultiStop = true ORDER BY r.date DESC")
    List<Ride> findMultiStopRidesByOwner(UUID ownerId);

    /**
     * Count active rides on a specific date.
     */
    @Query("SELECT COUNT(r) FROM Ride r WHERE r.status = 'ACTIVE' AND r.date = :date")
    long countActiveRidesByDate(LocalDate date);
}
