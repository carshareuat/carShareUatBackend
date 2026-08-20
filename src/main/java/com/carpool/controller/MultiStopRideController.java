package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.ride.CreateMultiStopRideRequest;
import com.carpool.dto.ride.RideDetailsDTO;
import com.carpool.dto.ride.RideSearchRequest;
import com.carpool.dto.ride.RideSearchResultDTO;
import com.carpool.service.MultiStopRideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller for multi-stop ride operations.
 * 
 * Endpoints:
 * - POST /api/v1/rides/multi-stop - Create a multi-stop ride
 * - GET /api/v1/rides/{rideId}/details - Get full ride details
 * - GET /api/v1/rides/search/multi-stop - Search for multi-stop rides
 * - GET /api/v1/rides/{rideId}/availability - Get segment availability
 */
@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
@Tag(name = "Rides - Multi-Stop", description = "Multi-stop ride creation, search, and management")
public class MultiStopRideController {

    private static final Logger log = LoggerFactory.getLogger(MultiStopRideController.class);

    private final MultiStopRideService rideService;

    // ========== CREATE MULTI-STOP RIDE ==========

    /**
     * Create a new multi-stop ride with segmented pricing.
     * 
     * Request body includes:
     * - List of stops (minimum 2)
     * - Pricing type (FIXED or SEGMENTED)
     * - Segment prices (if SEGMENTED)
     * - Vehicle details
     * - Seat capacity
     */
    @PostMapping("/multi-stop")
    @Operation(summary = "Create a multi-stop ride", description = "Create a new ride with multiple stops and segmented pricing")
    public ResponseEntity<?> createMultiStopRide(@Valid @RequestBody CreateMultiStopRideRequest request) {
        log.debug("POST /api/v1/rides/multi-stop: Creating multi-stop ride with {} stops", request.getStops().size());

        try {
            RideDetailsDTO ride = rideService.createMultiStopRide(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Multi-stop ride created successfully", ride));
        } catch (Exception e) {
            log.error("Error creating multi-stop ride", e);
            throw e;
        }
    }

    // ========== GET RIDE DETAILS ==========

    /**
     * Get complete ride details including all stops and segments.
     */
    @GetMapping("/{rideId}/details")
    @Operation(summary = "Get ride details", description = "Get complete ride information including all stops and segments")
    public ResponseEntity<?> getRideDetails(@PathVariable UUID rideId) {
        log.debug("GET /api/v1/rides/{}/details", rideId);

        RideDetailsDTO details = rideService.getRideDetails(rideId);
        return ResponseEntity.ok(ApiResponse.success("Ride details retrieved successfully", details));
    }

    // ========== SEARCH MULTI-STOP RIDES ==========

    /**
     * Search for available multi-stop rides.
     * 
     * Query parameters:
     * - from: Starting location (must exist as a stop)
     * - to: Destination location (must exist as a stop)
     * - date: Travel date (YYYY-MM-DD)
     * - seats: Number of seats required
     * - page: Page number (default: 0)
     * - size: Page size (default: 10)
     * - sortBy: Sort field (default: "date")
     * 
     * Matching logic:
     * 1. From location exists as a stop
     * 2. To location exists as a stop
     * 3. From stop order < To stop order
     * 4. All segments between them have sufficient seats
     */
    @GetMapping("/search/multi-stop")
    @Operation(summary = "Search multi-stop rides", description = "Find available rides for a journey segment")
    public ResponseEntity<?> searchMultiStopRides(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam LocalDate date,
            @RequestParam int seats,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy) {

        log.debug("GET /api/v1/rides/search/multi-stop: from={}, to={}, date={}, seats={}", from, to, date, seats);

        RideSearchRequest request = RideSearchRequest.builder()
            .fromLocation(from)
            .toLocation(to)
            .date(date)
            .seats(seats)
            .page(page)
            .size(size)
            .sortBy(sortBy)
            .build();

        Map<String, Object> results = rideService.searchMultiStopRides(request);
        return ResponseEntity.ok(ApiResponse.success("Rides found", results));
    }

    /**
     * Alternative search endpoint using POST for complex queries.
     */
    @PostMapping("/search/multi-stop")
    @Operation(summary = "Search multi-stop rides (POST)", description = "Find available rides using POST request")
    public ResponseEntity<?> searchMultiStopRidesPost(@Valid @RequestBody RideSearchRequest request) {
        log.debug("POST /api/v1/rides/search/multi-stop: from={}, to={}, date={}, seats={}",
            request.getFromLocation(), request.getToLocation(), request.getDate(), request.getSeats());

        Map<String, Object> results = rideService.searchMultiStopRides(request);
        return ResponseEntity.ok(ApiResponse.success("Rides found", results));
    }

    // ========== SEGMENT AVAILABILITY ==========

    /**
     * Get detailed availability information for a ride's segments.
     */
    @GetMapping("/{rideId}/availability")
    @Operation(summary = "Get ride availability", description = "Get segment-level seat availability for a ride")
    public ResponseEntity<?> getRideAvailability(@PathVariable UUID rideId) {
        log.debug("GET /api/v1/rides/{}/availability", rideId);

        RideDetailsDTO ride = rideService.getRideDetails(rideId);

        Map<String, Object> availability = Map.of(
            "rideId", rideId,
            "totalSeats", ride.getTotalSeats(),
            "availableSeats", ride.getAvailableSeats(),
            "segments", ride.getSegments()
        );

        return ResponseEntity.ok(ApiResponse.success("Availability retrieved", availability));
    }

    // ========== ROUTE PREVIEW ==========

    /**
     * Get route preview for a ride (simplified route summary).
     */
    @GetMapping("/{rideId}/route")
    @Operation(summary = "Get route preview", description = "Get route preview with all stops")
    public ResponseEntity<?> getRoutePreview(@PathVariable UUID rideId) {
        log.debug("GET /api/v1/rides/{}/route", rideId);

        RideDetailsDTO ride = rideService.getRideDetails(rideId);

        Map<String, Object> routeInfo = Map.of(
            "rideId", rideId,
            "routePreview", ride.getRoutePreview(),
            "totalStops", ride.getTotalStops(),
            "stops", ride.getStops()
        );

        return ResponseEntity.ok(ApiResponse.success("Route retrieved", routeInfo));
    }
}
