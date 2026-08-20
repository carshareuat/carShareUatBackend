package com.carpool.service.payment;

import java.util.Map;

public interface PaymentProviderClient {
    PaymentCheckout createCheckout(String subscriptionId, int amount, String currency, String successUrl, String cancelUrl, String ownerId);
    PaymentWebhookEvent parseWebhook(String signature, String payload);
    boolean validateWebhookSignature(String signature, String payload);
    String refund(String providerPaymentId, String reason);
    String providerName();
    Map<String, Object> toPayloadObject(String payload);
}
