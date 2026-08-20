package com.carpool.mapper;

import com.carpool.dto.ride.RideResponse;
import com.carpool.entity.Ride;
import org.springframework.stereotype.Component;

@Component
public class RideMapper {
    public RideResponse toResponse(Ride r) {
        return RideResponse.builder()
            .id(r.getId())
            .ownerId(r.getOwner().getId())
            .ownerName(r.getOwner().getName())
            .ownerAverageRating(r.getOwner().getAverageRating())
            .ownerRatingsCount(r.getOwner().getRatingsCount())
            .fromLocation(r.getFromLocation())
            .toLocation(r.getToLocation())
            .date(r.getDate())
            .startTime(r.getStartTime())
            .endTime(r.getEndTime())
            .price(r.getPrice())
            .carModel(r.getCarModel())
            .totalSeats(r.getTotalSeats())
            .availableSeats(r.getAvailableSeats())
            .status(r.getStatus())
            .femaleOnly(r.isFemaleOnly())
            .cancellationReason(r.getCancellationReason())
            .cancellationNote(r.getCancellationNote())
            .cancelledAt(r.getCancelledAt())
            .createdAt(r.getCreatedAt())
            .updatedAt(r.getUpdatedAt())
            .build();
    }
}
