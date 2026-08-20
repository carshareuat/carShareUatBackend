package com.carpool.mapper;

import com.carpool.dto.owner.OwnerResponse;
import com.carpool.entity.OwnerProfile;
import org.springframework.stereotype.Component;

@Component
public class OwnerMapper {
    public OwnerResponse toResponse(OwnerProfile o) {
        return OwnerResponse.builder()
            .id(o.getId())
            .userId(o.getUser().getId())
            .name(o.getName())
            .mobile(o.getMobile())
            .dateOfBirth(o.getUser().getDateOfBirth())
            .age(o.getUser().getAge())
            .verified(o.isVerified())
            .gender(o.getUser().getGender())
            .verificationStatus(o.getVerificationStatus())
            .profilePhotoUrl(o.getProfilePhotoUrl())
            .preferences(o.getPreferences())
            .averageRating(o.getAverageRating())
            .ratingsCount(o.getRatingsCount())
            .createdAt(o.getCreatedAt())
            .updatedAt(o.getUpdatedAt())
            .build();
    }
}
