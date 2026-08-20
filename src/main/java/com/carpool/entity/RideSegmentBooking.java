package com.carpool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Tracks passenger segment bookings for seat availability management.
 * 
 * When a passenger books a seat from Salem to Coimbatore on a multi-stop ride,
 * this entity records which booking occupies which segment.
 * 
 * This allows the system to:
 * - Calculate available seats per segment
 * - Prevent overbooking on specific segments
 * - Support different pricing for different routes
 * - Handle complex seat inventory scenarios
 */
@Getter
@Setter
@Entity
@Table(name = "ride_segment_bookings")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideSegmentBooking extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    private UUID id;

    /**
     * Reference to the ride being booked.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    /**
     * Reference to the booking (from bookings table).
     * Links this segment booking to the parent booking record.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * Starting stop of the booked segment.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_stop_id", nullable = false)
    private RideStop fromStop;

    /**
     * Ending stop of the booked segment.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "to_stop_id", nullable = false)
    private RideStop toStop;

    /**
     * Number of seats booked for this segment.
     * Occupies these seats for this segment only.
     */
    @Column(nullable = false)
    private int seatCount;
}
