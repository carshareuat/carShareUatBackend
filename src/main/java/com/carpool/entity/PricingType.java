package com.carpool.entity;

/**
 * Pricing strategy for rides.
 */
public enum PricingType {
    /**
     * Simple fixed price for the entire route.
     * Used for single-stop (point-to-point) rides.
     */
    FIXED,

    /**
     * Different prices for different journey segments.
     * Example: Pondicherry→Salem = ₹400, Salem→Erode = ₹200, etc.
     * Enables flexible pricing based on distance, demand, or stops.
     */
    SEGMENTED
}
