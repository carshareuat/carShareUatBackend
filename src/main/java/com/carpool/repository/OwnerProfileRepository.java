package com.carpool.repository;

import com.carpool.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, UUID> {
    Optional<OwnerProfile> findByUserId(UUID userId);
}
