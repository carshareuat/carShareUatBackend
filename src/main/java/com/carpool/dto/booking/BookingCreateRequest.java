package com.carpool.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class BookingCreateRequest {
    @NotNull
    private UUID rideId;
    @Min(1)
    private int seats;
}
