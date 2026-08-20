package com.carpool.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingCancelRequest {
    @NotBlank
    private String reason;
    private String note;
}
