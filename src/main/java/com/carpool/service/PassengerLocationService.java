package com.carpool.service;

import com.carpool.entity.PassengerLocation;
import com.carpool.repository.PassengerLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PassengerLocationService {

    private final PassengerLocationRepository repo;

    @Transactional
    public PassengerLocation saveOrUpdate(UUID passengerId, double lat, double lon) {
        Optional<PassengerLocation> existing = repo.findFirstByPassengerIdOrderByUpdatedAtDesc(passengerId);
        PassengerLocation loc = existing.orElseGet(PassengerLocation::new);
        loc.setPassengerId(passengerId);
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setUpdatedAt(Instant.now());
        return repo.save(loc);
    }

    public Optional<PassengerLocation> findByPassengerId(UUID passengerId) {
        return repo.findFirstByPassengerIdOrderByUpdatedAtDesc(passengerId);
    }

    public Map<String, Double> toMap(PassengerLocation loc) {
        return Map.of("lat", loc.getLatitude(), "lon", loc.getLongitude());
    }
}
