package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.ride.RideCreateRequest;
import com.carpool.dto.ride.RidePatchRequest;
import com.carpool.entity.RideStatus;
import com.carpool.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody RideCreateRequest request) {
        return ApiResponse.of(rideService.create(request));
    }

    @GetMapping
    public ApiResponse<?> list(
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) Integer passengers,
        @RequestParam(required = false) RideStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String sort
    ) {
        return ApiResponse.of(rideService.list(from, to, date, passengers, status, page, size, sort));
    }

    @GetMapping("/{rideId}")
    public ApiResponse<?> get(@PathVariable UUID rideId) {
        return ApiResponse.of(rideService.get(rideId));
    }

    @PatchMapping("/{rideId}")
    public ApiResponse<?> patch(@PathVariable UUID rideId, @RequestBody RidePatchRequest request) {
        return ApiResponse.of(rideService.update(rideId, request.getStatus(), request.getCancellationReason(), request.getCancellationNote()));
    }

    @DeleteMapping("/{rideId}")
    public ApiResponse<?> delete(@PathVariable UUID rideId) {
        return ApiResponse.of(rideService.delete(rideId));
    }

    @GetMapping("/{rideId}/location")
    public ApiResponse<?> location(@PathVariable UUID rideId) {
        return ApiResponse.of(rideService.getLocation(rideId));
    }

    @GetMapping("/{rideId}/passenger-location")
    public ApiResponse<?> passengerLocation(@PathVariable UUID rideId) {
        return ApiResponse.of(rideService.getPassengerLocation(rideId));
    }
}
