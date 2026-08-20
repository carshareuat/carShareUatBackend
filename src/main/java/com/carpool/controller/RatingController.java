package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.rating.RatingRequest;
import com.carpool.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/bookings/{bookingId}/rating")
    public ApiResponse<?> submit(@PathVariable UUID bookingId, @Valid @RequestBody RatingRequest request) {
        return ApiResponse.of(ratingService.submit(bookingId, request));
    }

}
