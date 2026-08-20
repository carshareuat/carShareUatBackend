package com.carpool.repository;

import com.carpool.entity.Booking;
import com.carpool.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    boolean existsByRideIdAndPassengerIdAndStatusIn(UUID rideId, UUID passengerId, List<BookingStatus> statuses);
    List<Booking> findByRideId(UUID rideId);
    List<Booking> findByPassengerId(UUID passengerId);
    Optional<Booking> findByIdAndPassengerId(UUID bookingId, UUID passengerId);
    List<Booking> findByRideIdAndStatusIn(UUID rideId, List<BookingStatus> statuses);
    boolean existsByPassengerIdAndStatusAndRatedFalse(UUID passengerId, BookingStatus status);

    @Query("select count(b) > 0 from Booking b where b.ride.id = :rideId and b.passenger.id = :passengerId and b.status in :statuses")
    boolean hasActiveBooking(UUID rideId, UUID passengerId, List<BookingStatus> statuses);
}
