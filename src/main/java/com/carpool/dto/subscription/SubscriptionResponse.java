package com.carpool.dto.subscription;

import com.carpool.entity.PaymentProvider;
import com.carpool.entity.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SubscriptionResponse {
    private UUID id;
    private UUID ownerId;
    private String ownerName;
    private String ownerMobile;
    private LocalDate ownerDateOfBirth;
    private Integer ownerAge;
    private int amount;
    private String currency;
    private PaymentProvider provider;
    private String providerPaymentId;
    private String utrNumber;
    private Instant paymentDate;
    private Instant reviewedAt;
    private String rejectionComment;
    private SubscriptionStatus status;
    private Instant startsAt;
    private Instant expiresAt;
    private String planName;
    private java.util.UUID planId;
    private Instant createdAt;
    private Instant updatedAt;
}
