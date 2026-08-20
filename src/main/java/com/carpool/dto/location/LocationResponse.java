package com.carpool.dto.location;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LocationResponse {
    private UUID id;
    private String state;
    private String district;
}
