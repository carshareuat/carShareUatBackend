package com.carpool.repository;

import com.carpool.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    List<Location> findByStateIgnoreCaseContainingAndDistrictIgnoreCaseContaining(String state, String district);
    List<Location> findByDistrictIgnoreCaseContaining(String district);
}
