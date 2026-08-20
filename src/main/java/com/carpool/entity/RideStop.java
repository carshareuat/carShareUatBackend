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

import java.time.LocalTime;
import java.util.UUID;

/**
 * Represents a stop in a multi-stop ride route.
 * 
 * Each stop has:
 * - Sequential order in the route
 * - Location name (place/city)
 * - Arrival and departure times
 * 
 * Example for route Pondicherry → Villupuram → Salem → Erode → Coimbatore:
 * Stop 0: Pondicherry    (departure: 06:00 AM,      arrival: null)
 * Stop 1: Villupuram     (arrival: 07:00 AM,  departure: 07:10 AM)
 * Stop 2: Salem          (arrival: 10:00 AM,  departure: 10:15 AM)
 * Stop 3: Erode          (arrival: 11:30 AM,  departure: 11:40 AM)
 * Stop 4: Coimbatore     (arrival: 01:00 PM,  departure: null)
 */
@Getter
@Setter
@Entity
@Table(name = "ride_stops")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideStop extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    /**
     * Sequential order of this stop in the route.
     * 0 = origin, 1, 2, 3... = intermediate stops and destination
     */
    @Column(nullable = false)
    private int stopOrder;

    /**
     * Name of the location/city at this stop (start place or drop place).
     * Example: "Pondicherry", "Villupuram", "Salem", etc.
     */
    @Column(nullable = false, length = 150)
    private String locationName;

    @Column(precision = 10, scale = 6)
    private java.math.BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private java.math.BigDecimal longitude;

    /**
     * Expected arrival time at this stop.
     * NULL for the first stop (origin).
     */
    @Column(nullable = true)
    private LocalTime arrivalTime;

    /**
     * Expected departure time from this stop.
     * NULL for the last stop (final destination).
     */
    @Column(nullable = true)
    private LocalTime departureTime;

    @Column
    private Integer stopDurationMinutes;
}
