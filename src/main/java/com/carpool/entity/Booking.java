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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @Column(nullable = false, length = 20)
    private String passengerMobile;

    @Column(nullable = false)
    private int seats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(length = 255)
    private String cancellationReason;

    @Column(length = 500)
    private String cancellationNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CancelledBy cancelledBy;

    private Instant cancelledAt;

    @Column(nullable = false)
    private boolean needsRating = false;

    @Column(nullable = false)
    private boolean rated = false;

    // ========== Multi-Stop Segment Support ==========

    /**
     * Starting stop for segment booking (only used for multi-stop rides).
     * For simple rides, this is null.
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "from_stop_id", nullable = true)
    private RideStop fromStop;

    /**
     * Ending stop for segment booking (only used for multi-stop rides).
     * For simple rides, this is null.
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "to_stop_id", nullable = true)
    private RideStop toStop;

    /**
     * Price for the booked segment (only used for multi-stop rides).
     * For simple rides, this is null (use ride.price instead).
     */
    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal segmentPrice;

    /**
     * List of segment bookings for this booking.
     * Multiple segment bookings can be created if a passenger books multiple segments.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RideSegmentBooking> segmentBookings = new ArrayList<>();
}
