package com.carpool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a journey segment between two stops with pricing and seat availability.
 * 
 * A segment is a connection from one stop to another (not necessarily consecutive).
 * For a 4-stop ride (A→B→C→D), all possible segments are:
 * - A→B, A→C, A→D
 * - B→C, B→D
 * - C→D
 * Total: 6 segments (n*(n-1)/2 where n=4)
 * 
 * Each segment has independent:
 * - Price (for that specific journey)
 * - Available seats (tracked per segment for seat inventory management)
 * - Duration (optional)
 */
@Getter
@Setter
@Entity
@Table(name = "ride_segments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ride_id", "from_stop_id", "to_stop_id"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSegment extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    /**
     * Starting stop of this segment.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private RideStop fromStop;

    /**
     * Ending stop of this segment.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private RideStop toStop;

    /**
     * Sequential segment number for ordering.
     * Helps identify which segment is first, second, etc.
     */
    @Column(nullable = false)
    private int segmentOrder;

    /**
     * Price for traveling this segment.
     * Can be set manually by driver or calculated automatically.
     * Example: Pondicherry → Salem = ₹400
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Current available seats for this segment.
     * Decreases as passengers book this segment.
     * 
     * Example: If ride has 4 total seats and passenger books Salem→Erode,
     * only this segment's available_seats decreases, not others.
     */
    @Column(nullable = false)
    private int availableSeats;

    /**
     * Total seats allocated to this ride (reference for availability calculation).
     * Same for all segments in the ride.
     */
    @Column(nullable = false)
    private int totalSeats;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal distanceKm;

    /**
     * Expected travel duration in minutes for this segment (optional).
     * Calculated as: departure_time_of_to_stop - arrival_time_of_from_stop
     */
    @Column
    private Integer durationMinutes;
}
