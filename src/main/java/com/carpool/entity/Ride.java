package com.carpool.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "rides")
public class Ride extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerProfile owner;

    @Column(nullable = false, length = 150)
    private String fromLocation;

    @Column(nullable = false, length = 150)
    private String toLocation;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 100)
    private String carModel;

    @Column(nullable = false)
    private int totalSeats;

    @Column(nullable = false)
    private int availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RideStatus status = RideStatus.ACTIVE;

    @Column(length = 255)
    private String cancellationReason;

    @Column(length = 500)
    private String cancellationNote;

    private Instant cancelledAt;

    @Column(nullable = false)
    private boolean femaleOnly = false;

    // ========== Multi-Stop Route Support ==========

    /**
     * Flag indicating if this is a multi-stop ride.
     * true = multi-stop route with multiple stops defined
     * false = simple point-to-point ride (legacy)
     */
    @Column(nullable = false)
    private boolean isMultiStop = false;

    public void setMultiStop(boolean multiStop) {
        this.isMultiStop = multiStop;
    }

    public boolean isMultiStop() {
        return this.isMultiStop;
    }

    public void setIsMultiStop(boolean isMultiStop) {
        this.isMultiStop = isMultiStop;
    }

    /**
     * Total number of stops in this ride.
     * For a simple ride: 2 (origin + destination)
     * For multi-stop: 2+ (origin + intermediate stops + destination)
     */
    @Column(nullable = false)
    private int totalStops = 2;

    /**
     * Type of pricing strategy for this ride.
     * FIXED = single price for entire route (legacy behavior)
     * SEGMENTED = different prices for different route segments
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PricingType pricingType = PricingType.FIXED;

    /**
     * List of all stops in this ride (in order).
     * Populated only for multi-stop rides.
     */
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RideStop> stops = new ArrayList<>();

    /**
     * List of all segments for this ride.
     * Each segment represents a journey from one stop to another.
     * Populated for multi-stop rides.
     */
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RideSegment> segments = new ArrayList<>();

    /**
     * List of segment bookings for seat availability tracking.
     */
    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RideSegmentBooking> segmentBookings = new ArrayList<>();
}
