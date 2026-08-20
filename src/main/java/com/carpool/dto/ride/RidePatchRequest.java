package com.carpool.dto.ride;

import com.carpool.entity.RideStatus;
import lombok.Data;

@Data
public class RidePatchRequest {
    private RideStatus status;
    private String cancellationReason;
    private String cancellationNote;
}
