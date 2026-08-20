package com.carpool.repository;

import com.carpool.entity.PassengerLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PassengerLocationRepository extends JpaRepository<PassengerLocation, UUID> {
    // Return the latest record only; this avoids NonUniqueResultException when
    // multiple passenger location rows already exist for the same passenger.
    Optional<PassengerLocation> findFirstByPassengerIdOrderByUpdatedAtDesc(UUID passengerId);
}
