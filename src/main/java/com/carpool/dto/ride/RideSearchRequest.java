package com.carpool.dto.ride;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for searching available rides with multi-stop support.
 * 
 * Passengers provide:
 * - From location (must exist as a stop in the ride)
 * - To location (must exist as a stop after the from location)
 * - Travel date
 * - Number of seats required
 * 
 * The system finds all rides where:
 * 1. From location exists as a stop
 * 2. To location exists as a stop
 * 3. From stop order < To stop order
 * 4. All segments between them have sufficient seats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSearchRequest {

    /**
     * Starting location name.
     * Example: "Salem"
     */
    @NotBlank(message = "From location is required")
    private String fromLocation;

    /**
     * Destination location name.
     * Example: "Coimbatore"
     */
    @NotBlank(message = "To location is required")
    private String toLocation;

    /**
     * Travel date.
     */
    @NotNull(message = "Travel date is required")
    private LocalDate date;

    /**
     * Number of seats required.
     */
    @NotNull(message = "Seat count is required")
    @Min(value = 1, message = "At least 1 seat is required")
    private int seats;

    /**
     * Page number for pagination (default: 0).
     */
    @Builder.Default
    private int page = 0;

    /**
     * Page size for pagination (default: 10, max: 50).
     */
    @Builder.Default
    private int size = 10;

    /**
     * Sort field (default: "date", options: "date", "price", "availableSeats").
     */
    @Builder.Default
    private String sortBy = "date";
}
