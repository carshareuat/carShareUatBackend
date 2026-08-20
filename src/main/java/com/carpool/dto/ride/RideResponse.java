package com.carpool.dto.ride;

import com.carpool.entity.RideStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class RideResponse {
    private UUID id;
    private UUID ownerId;
    private String ownerName;
    private BigDecimal ownerAverageRating;
    private long ownerRatingsCount;
    private String fromLocation;
    private String toLocation;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal price;
    private String carModel;
    private int totalSeats;
    private int availableSeats;
    private RideStatus status;
    private String cancellationReason;
    private String cancellationNote;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean femaleOnly;
}
