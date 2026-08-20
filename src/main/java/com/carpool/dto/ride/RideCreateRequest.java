package com.carpool.dto.ride;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class RideCreateRequest {
    @NotBlank
    private String fromLocation;
    @NotBlank
    private String toLocation;
    @NotNull
    private LocalDate date;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal price;
    private String carModel;
    @Min(1)
    private int totalSeats;
    private Boolean femaleOnly;
}
