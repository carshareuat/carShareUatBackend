package com.carpool.dto.ride;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for search results containing detailed ride information.
 * 
 * Includes:
 * - Ride ID and driver info
 * - Selected segment times and duration
 * - Pricing information
 * - Seat availability
 * - Complete route preview
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSearchResultDTO {

    /**
     * Ride ID.
     */
    private UUID rideId;

    /**
     * Driver ID.
     */
    private UUID driverId;

    /**
     * Driver name.
     */
    private String driverName;

    /**
     * Driver average rating (0-5).
     */
    private BigDecimal driverAverageRating;

    /**
     * Number of ratings for the driver.
     */
    private long driverRatingsCount;

    /**
     * Vehicle model.
     */
    private String vehicleModel;

    /**
     * Travel date (as string, YYYY-MM-DD).
     */
    private String travelDate;

    /**
     * Starting location of the selected segment.
     */
    private String fromLocation;

    /**
     * Ending location of the selected segment.
     */
    private String toLocation;

    /**
     * Departure time from the selected from location.
     */
    private LocalTime departureTime;

    /**
     * Arrival time at the selected to location.
     */
    private LocalTime arrivalTime;

    /**
     * Travel duration as formatted string (e.g., "2h 45m").
     */
    private String travelDuration;

    /**
     * Price for the selected segment route.
     */
    private BigDecimal price;

    /**
     * Number of available seats for the selected segment.
     */
    private int availableSeats;

    /**
     * Total seats in the vehicle.
     */
    private int totalSeats;

    /**
     * Complete route preview showing all stops.
     * Example: "Pondicherry → Villupuram → Salem → Erode → Coimbatore"
     */
    private String routePreview;

    /**
     * List of all stops in the ride (for detailed itinerary).
     */
    private List<RouteStopDetail> routeStops;

    /**
     * Whether ride is female-only.
     */
    private boolean femaleOnly;

    /**
     * Distance of the selected segment in KM.
     */
    private BigDecimal distanceKm;

    /**
     * Detail about a single stop in the route.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteStopDetail {

        /**
         * Stop order.
         */
        private int stopOrder;

        /**
         * Location name.
         */
        private String locationName;

        /**
         * Arrival time.
         */
        private LocalTime arrivalTime;

        /**
         * Departure time.
         */
        private LocalTime departureTime;

        /**
         * Whether this is the selected from location.
         */
        private boolean isFromStop;

        /**
         * Whether this is the selected to location.
         */
        private boolean isToStop;
    }
}
