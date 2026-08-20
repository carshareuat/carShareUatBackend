package com.carpool.service;

import com.carpool.entity.OwnerLocation;
import com.carpool.repository.OwnerLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerLocationService {

    private final OwnerLocationRepository repo;

    @Transactional
    public OwnerLocation saveOrUpdate(UUID ownerId, double lat, double lon) {
        // Use repository method that returns the most recent location to avoid
        // NonUniqueResultException if multiple rows exist for the same ownerId.
        Optional<OwnerLocation> existing = repo.findFirstByOwnerIdOrderByUpdatedAtDesc(ownerId);
        OwnerLocation loc = existing.orElseGet(OwnerLocation::new);
        loc.setOwnerId(ownerId);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setUpdatedAt(Instant.now());
        return repo.save(loc);
    }

    public Optional<OwnerLocation> findByOwnerId(UUID ownerId) {
        return repo.findFirstByOwnerIdOrderByUpdatedAtDesc(ownerId);
    }

    public Map<String, Double> toMap(OwnerLocation loc) {
        return Map.of("lat", loc.getLatitude(), "lon", loc.getLongitude());
    }
}
