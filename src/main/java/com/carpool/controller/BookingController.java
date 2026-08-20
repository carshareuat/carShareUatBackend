package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.booking.BookingCancelRequest;
import com.carpool.dto.booking.BookingCreateRequest;
import com.carpool.dto.booking.BookingDecisionRequest;
import com.carpool.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    public ApiResponse<?> create(@Valid @RequestBody BookingCreateRequest request) {
        return ApiResponse.of(bookingService.create(request));
    }

    @GetMapping("/bookings/me")
    public ApiResponse<?> my() {
        return ApiResponse.of(bookingService.myBookings());
    }

    @GetMapping("/bookings/{bookingId}")
    public ApiResponse<?> get(@PathVariable UUID bookingId) {
        return ApiResponse.of(bookingService.get(bookingId));
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    public ApiResponse<?> cancel(@PathVariable UUID bookingId, @Valid @RequestBody BookingCancelRequest request) {
        return ApiResponse.of(bookingService.cancel(bookingId, request));
    }

    @GetMapping("/rides/{rideId}/bookings")
    public ApiResponse<?> byRide(@PathVariable UUID rideId) {
        return ApiResponse.of(bookingService.rideBookings(rideId));
    }

    @GetMapping("/rides/{rideId}/confirmed-passengers")
    public ApiResponse<?> confirmedPassengers(@PathVariable UUID rideId) {
        return ApiResponse.of(bookingService.confirmedPassengers(rideId));
    }

    @PatchMapping("/bookings/{bookingId}/decision")
    public ApiResponse<?> decision(@PathVariable UUID bookingId, @Valid @RequestBody BookingDecisionRequest request) {
        return ApiResponse.of(bookingService.decide(bookingId, request));
    }
}
