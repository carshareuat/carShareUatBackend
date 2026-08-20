package com.carpool.dto.ride;

import com.carpool.entity.PricingType;
import com.carpool.entity.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO containing complete ride details including all stops and segments.
 * Used for:
 * - Getting full ride information
 * - Driver viewing their posted rides
 * - Displaying ride details to passengers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideDetailsDTO {

    /**
     * Ride ID.
     */
    private UUID id;

    /**
     * Owner/Driver ID.
     */
    private UUID ownerId;

    /**
     * Owner/Driver name.
     */
    private String ownerName;

    /**
     * Driver average rating.
     */
    private BigDecimal ownerAverageRating;

    /**
     * Number of driver ratings.
     */
    private long ownerRatingsCount;

    /**
     * Travel date.
     */
    private LocalDate date;

    /**
     * Car model.
     */
    private String carModel;

    /**
     * Total seats available.
     */
    private int totalSeats;

    /**
     * Overall available seats.
     */
    private int availableSeats;

    /**
     * Ride status.
     */
    private RideStatus status;

    /**
     * Pricing type (FIXED or SEGMENTED).
     */
    private PricingType pricingType;

    /**
     * Whether this is a multi-stop ride.
     */
    private boolean isMultiStop;

    /**
     * Total number of stops.
     */
    private int totalStops;

    /**
     * Whether ride is female-only.
     */
    private Boolean femaleOnly;

    /**
     * List of all stops in order.
     */
    private List<RideStopDTO> stops;

    /**
     * List of all segments with pricing.
     */
    private List<RideSegmentDTO> segments;

    /**
     * Price for simple/fixed rides (FIXED pricing type).
     */
    private BigDecimal price;

    /**
     * Cancellation reason (if cancelled).
     */
    private String cancellationReason;

    /**
     * Cancellation note (if cancelled).
     */
    private String cancellationNote;

    /**
     * Timestamp when cancelled.
     */
    private Instant cancelledAt;

    /**
     * When the ride was created.
     */
    private Instant createdAt;

    /**
     * When the ride was last updated.
     */
    private Instant updatedAt;

    /**
     * Route preview (e.g., "Pondicherry → Villupuram → Salem → Erode → Coimbatore").
     */
    private String routePreview;
}
