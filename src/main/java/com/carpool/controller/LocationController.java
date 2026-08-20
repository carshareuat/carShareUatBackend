package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.location.LocationResponse;
import com.carpool.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationRepository locationRepository;

    @GetMapping
    public ApiResponse<?> search(@RequestParam(required = false) String query, @RequestParam(required = false) String state) {
        String q = query == null ? "" : query.trim();
        String s = state == null ? "" : state.trim();
        List<?> items;
        if (!s.isEmpty()) {
            items = locationRepository.findByStateIgnoreCaseContainingAndDistrictIgnoreCaseContaining(s, q).stream()
                .map(l -> LocationResponse.builder().id(l.getId()).state(l.getState()).district(l.getDistrict()).build()).collect(Collectors.toList());
        } else {
            items = locationRepository.findByDistrictIgnoreCaseContaining(q).stream()
                .map(l -> LocationResponse.builder().id(l.getId()).state(l.getState()).district(l.getDistrict()).build()).collect(Collectors.toList());
        }
        return ApiResponse.of(items);
    }
}
