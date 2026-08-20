package com.carpool.dto.owner;

import com.carpool.entity.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class OwnerResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String mobile;
    private LocalDate dateOfBirth;
    private String gender;
    private Integer age;
    private boolean verified;
    private VerificationStatus verificationStatus;
    private String profilePhotoUrl;
    private String preferences;
    private BigDecimal averageRating;
    private long ratingsCount;
    private Instant createdAt;
    private Instant updatedAt;
}
