package com.carpool.dto.ride;

import com.carpool.entity.PricingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for creating a new multi-stop ride.
 * 
 * Request includes:
 * - List of stops (minimum 2)
 * - Pricing strategy (FIXED or SEGMENTED)
 * - Either single price or segment prices
 * - Vehicle and driver information
 * - Seat capacity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMultiStopRideRequest {

    /**
     * List of stops in order (minimum 2).
     */
    @NotEmpty(message = "At least 2 stops are required")
    private List<RideStopDTO> stops;

    /**
     * Travel date.
     */
    @NotNull(message = "Travel date is required")
    private LocalDate date;

    /**
     * Pricing strategy: FIXED or SEGMENTED.
     */
    @NotNull(message = "Pricing type is required")
    private PricingType pricingType;

    /**
     * List of segment prices (required if pricingType is SEGMENTED).
     * If pricingType is FIXED, pass a single segment with from=0, to=lastStop, and price=totalPrice.
     */
    private List<SegmentPriceDTO> segmentPrices;

    /**
     * Car model.
     */
    private String carModel;

    /**
     * Total seats available in the vehicle.
     */
    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    private int totalSeats;

    /**
     * Whether to show ride only to female passengers.
     */
    private Boolean femaleOnly = false;

    /**
     * DTO for defining price for a segment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentPriceDTO {

        /**
         * Stop order of starting point (0 = origin).
         */
        @Min(0)
        private int fromStopOrder;

        /**
         * Stop order of ending point.
         */
        @Min(0)
        private int toStopOrder;

        /**
         * Price for this segment.
         */
        private java.math.BigDecimal price;

        /**
         * Segment distance in kilometers, when calculated from distance-based pricing.
         */
        private java.math.BigDecimal distanceKm;

        /**
         * Price per kilometer for distance-based pricing.
         */
        private java.math.BigDecimal pricePerKm;

        /**
         * Fixed base fare for distance-based pricing.
         */
        private java.math.BigDecimal baseFare;
    }
}
