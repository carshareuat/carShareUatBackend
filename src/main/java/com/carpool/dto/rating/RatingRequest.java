package com.carpool.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RatingRequest {
    @Min(1)
    @Max(5)
    private int rating;
    private String note;
}
