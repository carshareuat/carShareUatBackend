package com.carpool.repository;

import com.carpool.entity.OwnerLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OwnerLocationRepository extends JpaRepository<OwnerLocation, UUID> {
    // Return the most recent location for an owner. Using a "findFirstBy...OrderByUpdatedAtDesc"
    // prevents NonUniqueResultException when multiple rows exist for the same ownerId by
    // limiting the query to a single result.
    Optional<OwnerLocation> findFirstByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
