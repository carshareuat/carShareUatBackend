package com.carpool.dto.booking;

import com.carpool.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingDecisionRequest {
    @NotNull
    private BookingStatus status;
}
