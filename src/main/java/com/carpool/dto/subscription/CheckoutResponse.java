package com.carpool.dto.subscription;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CheckoutResponse {
    private UUID subscriptionId;
    private int amount;
    private String currency;
    private String provider;
    private String checkoutUrl;
    private String providerOrderId;
}
