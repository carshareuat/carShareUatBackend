package com.carpool.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Profile({"default", "dev", "local"})
public class MockPaymentProviderClient implements PaymentProviderClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PaymentCheckout createCheckout(String subscriptionId, int amount, String currency, String successUrl, String cancelUrl, String ownerId) {
        // For local/dev flows prefer the manual UTR/UPI verification screen instead of redirecting
        // to a mock external checkout. Returning null for checkoutUrl makes the frontend
        // navigate to the internal payment/UTR page.
        return new PaymentCheckout(null,
            "mock_order_" + UUID.randomUUID(), "MOCK");
    }

    @Override
    public PaymentWebhookEvent parseWebhook(String signature, String payload) {
        Map<String, Object> map = toPayloadObject(payload);
        return new PaymentWebhookEvent(
            String.valueOf(map.getOrDefault("providerPaymentId", "mock_pay_" + UUID.randomUUID())),
            String.valueOf(map.getOrDefault("providerOrderId", "mock_order")),
            String.valueOf(map.getOrDefault("status", "PAID")),
            Integer.parseInt(String.valueOf(map.getOrDefault("amount", 19900))),
            String.valueOf(map.getOrDefault("currency", "INR")),
            String.valueOf(map.getOrDefault("ownerId", ""))
        );
    }

    @Override
    public boolean validateWebhookSignature(String signature, String payload) {
        return true;
    }

    @Override
    public String refund(String providerPaymentId, String reason) {
        return "mock_refund_" + UUID.randomUUID();
    }

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public Map<String, Object> toPayloadObject(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid webhook payload");
        }
    }
}
