package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.entity.Booking;
import com.carpool.service.MultiStopBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller for multi-stop ride booking operations.
 * 
 * Endpoints:
 * - POST /api/v1/bookings/segment - Book a ride segment
 * - GET /api/v1/bookings/{bookingId}/details - Get booking details
 * - DELETE /api/v1/bookings/{bookingId} - Cancel a booking
 * - GET /api/v1/rides/{rideId}/segments/{fromStopId}/to/{toStopId}/availability - Get segment availability
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings - Multi-Stop", description = "Multi-stop ride booking, seat management, and cancellation")
public class MultiStopBookingController {

    private static final Logger log = LoggerFactory.getLogger(MultiStopBookingController.class);

    private final MultiStopBookingService bookingService;

    // ========== BOOK SEGMENT ==========

    /**
     * Book a segment of a multi-stop ride.
     * 
     * Query parameters:
     * - rideId: Ride ID
     * - passengerId: Passenger user ID
     * - passengerMobile: Passenger mobile number
     * - fromLocation: Starting location name
     * - toLocation: Destination location name
     * - seats: Number of seats to book
     */
    @PostMapping("/segment")
    @Operation(summary = "Book a ride segment", description = "Book a specific segment of a multi-stop ride")
    public ResponseEntity<?> bookSegment(
            @RequestParam UUID rideId,
            @RequestParam UUID passengerId,
            @RequestParam String passengerMobile,
            @RequestParam String fromLocation,
            @RequestParam String toLocation,
            @RequestParam int seats) {

        log.debug("POST /api/v1/bookings/segment: ride={}, passenger={}, from={}, to={}, seats={}",
            rideId, passengerId, fromLocation, toLocation, seats);

        try {
            Booking booking = bookingService.bookSegment(rideId, passengerId, passengerMobile,
                fromLocation, toLocation, seats);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Segment booked successfully", Map.of(
                    "bookingId", booking.getId(),
                    "rideId", booking.getRide().getId(),
                    "status", booking.getStatus(),
                    "seats", booking.getSeats(),
                    "price", booking.getSegmentPrice(),
                    "createdAt", booking.getCreatedAt()
                )));
        } catch (Exception e) {
            log.error("Error booking segment", e);
            throw e;
        }
    }

    // ========== GET BOOKING DETAILS ==========

    /**
     * Get booking details.
     */
    @GetMapping("/{bookingId}/details")
    @Operation(summary = "Get booking details", description = "Get detailed information about a booking")
    public ResponseEntity<?> getBookingDetails(@PathVariable UUID bookingId) {
        log.debug("GET /api/v1/bookings/{}/details", bookingId);

        MultiStopBookingService.BookingDetails details = bookingService.getBookingDetails(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking details retrieved", details));
    }

    // ========== CANCEL BOOKING ==========

    /**
     * Cancel a booking and release reserved seats.
     */
    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancel booking", description = "Cancel a booking and release seats back to the ride")
    public ResponseEntity<?> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestParam(defaultValue = "Passenger requested cancellation") String reason) {

        log.debug("DELETE /api/v1/bookings/{}?reason={}", bookingId, reason);

        try {
            bookingService.cancelBooking(bookingId, reason);
            return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully"));
        } catch (Exception e) {
            log.error("Error cancelling booking", e);
            throw e;
        }
    }

    // ========== SEGMENT AVAILABILITY ==========

    /**
     * Get availability for a specific segment.
     */
    @GetMapping("/rides/{rideId}/segments/{fromStopId}/to/{toStopId}/availability")
    @Operation(summary = "Get segment availability", description = "Get available seats for a specific segment")
    public ResponseEntity<?> getSegmentAvailability(
            @PathVariable UUID rideId,
            @PathVariable UUID fromStopId,
            @PathVariable UUID toStopId) {

        log.debug("GET /api/v1/bookings/rides/{}/segments/{}/to/{}/availability", rideId, fromStopId, toStopId);

        int availableSeats = bookingService.getSegmentAvailability(rideId, fromStopId, toStopId);
        return ResponseEntity.ok(ApiResponse.success("Availability retrieved", Map.of(
            "availableSeats", availableSeats
        )));
    }

    /**
     * Get occupancy details for a segment.
     */
    @GetMapping("/rides/{rideId}/segments/{fromStopId}/to/{toStopId}/occupancy")
    @Operation(summary = "Get segment occupancy", description = "Get occupancy details for a segment")
    public ResponseEntity<?> getSegmentOccupancy(
            @PathVariable UUID rideId,
            @PathVariable UUID fromStopId,
            @PathVariable UUID toStopId) {

        log.debug("GET /api/v1/bookings/rides/{}/segments/{}/to/{}/occupancy", rideId, fromStopId, toStopId);

        MultiStopBookingService.SegmentOccupancy occupancy = bookingService.getSegmentOccupancy(rideId, fromStopId, toStopId);
        return ResponseEntity.ok(ApiResponse.success("Occupancy retrieved", occupancy));
    }

    /**
     * Get journey availability (available seats across all segments of a journey).
     */
    @GetMapping("/rides/{rideId}/journey-availability")
    @Operation(summary = "Get journey availability", description = "Get available seats considering all segments of a journey")
    public ResponseEntity<?> getJourneyAvailability(
            @PathVariable UUID rideId,
            @RequestParam int fromStopOrder,
            @RequestParam int toStopOrder) {

        log.debug("GET /api/v1/bookings/rides/{}/journey-availability?from={}&to={}",
            rideId, fromStopOrder, toStopOrder);

        int availableSeats = bookingService.getJourneyAvailability(rideId, fromStopOrder, toStopOrder);
        return ResponseEntity.ok(ApiResponse.success("Journey availability retrieved", Map.of(
            "availableSeats", availableSeats,
            "fromStopOrder", fromStopOrder,
            "toStopOrder", toStopOrder
        )));
    }
}
