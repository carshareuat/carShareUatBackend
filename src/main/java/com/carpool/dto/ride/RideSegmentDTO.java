package com.carpool.dto.ride;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for a ride segment (journey from one stop to another).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSegmentDTO {

    /**
     * UUID of the segment (only for responses).
     */
    private UUID id;

    /**
     * UUID of the starting stop.
     */
    @NotNull(message = "From stop ID is required")
    private UUID fromStopId;

    /**
     * UUID of the ending stop.
     */
    @NotNull(message = "To stop ID is required")
    private UUID toStopId;

    /**
     * Price for this segment.
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    /**
     * Available seats for this segment.
     */
    @NotNull(message = "Available seats is required")
    @Min(value = 1, message = "Available seats must be at least 1")
    private int availableSeats;

    /**
     * Total seats in the ride.
     */
    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;

    /**
     * Distance of the segment in km (optional).
     */
    private java.math.BigDecimal distanceKm;

    /**
     * Travel duration in minutes (optional).
     */
    private Integer durationMinutes;
}
