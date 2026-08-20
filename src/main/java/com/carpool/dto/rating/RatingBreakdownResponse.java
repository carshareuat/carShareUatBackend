package com.carpool.dto.rating;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class RatingBreakdownResponse {
    private BigDecimal average;
    private long count;
    private Map<Integer, Long> stars;
}
