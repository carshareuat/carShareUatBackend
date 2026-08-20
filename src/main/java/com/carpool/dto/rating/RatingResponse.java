package com.carpool.dto.rating;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class RatingResponse {
    private UUID id;
    private UUID bookingId;
    private UUID ownerId;
    private UUID passengerId;
    private int rating;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
}
