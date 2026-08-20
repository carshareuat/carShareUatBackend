package com.carpool.dto.ride;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * DTO for a single stop in a multi-stop ride route.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideStopDTO {

    /**
     * Sequential order of this stop in the route.
     * 0 = origin, 1, 2, 3... = intermediate/destination
     */
    @Min(0)
    private int stopOrder;

    /**
     * Name of the location (start place or drop place).
     * Example: "Pondicherry", "Villupuram", etc.
     */
    @NotBlank(message = "Location name is required")
    private String locationName;

    /**
     * Latitude of this stop.
     */
    private BigDecimal latitude;

    /**
     * Longitude of this stop.
     */
    private BigDecimal longitude;

    /**
     * Arrival time at this stop.
     * NULL for first stop (origin).
     */
    private LocalTime arrivalTime;

    /**
     * Departure time from this stop.
     * NULL for last stop (destination).
     */
    private LocalTime departureTime;

    /**
     * Stop dwell time in minutes.
     */
    private Integer stopDurationMinutes;
}
