package com.carpool.dto.booking;

import com.carpool.entity.BookingStatus;
import com.carpool.entity.CancelledBy;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private UUID id;
    private UUID rideId;
    private UUID passengerId;
    private String passengerMobile;
    private String passengerName;
    private Integer passengerAge;
    private int seats;
    private BookingStatus status;
    private String cancellationReason;
    private String cancellationNote;
    private CancelledBy cancelledBy;
    private Instant cancelledAt;
    private boolean needsRating;
    private boolean rated;
    private Instant createdAt;
    private Instant updatedAt;
}
