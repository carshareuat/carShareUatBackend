package com.carpool.dto.subscription;

import lombok.Data;

@Data
public class WebhookRequest {
    private String event;
    private String providerPaymentId;
    private String providerOrderId;
    private String status;
    private Integer amount;
    private String currency;
    private String ownerId;
}
