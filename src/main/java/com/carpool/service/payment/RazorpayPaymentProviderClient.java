package com.carpool.service.payment;

import com.carpool.config.AppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("prod")
public class RazorpayPaymentProviderClient implements PaymentProviderClient {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RazorpayPaymentProviderClient(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public PaymentCheckout createCheckout(String subscriptionId, int amount, String currency, String successUrl, String cancelUrl, String ownerId) {
        String orderId = "order_" + UUID.randomUUID();
        // Integration point: replace with actual Razorpay order API call.
        String checkoutUrl = "https://api.razorpay.com/v1/checkout/embedded?order_id=" + orderId;
        return new PaymentCheckout(checkoutUrl, orderId, "RAZORPAY");
    }

    @Override
    public PaymentWebhookEvent parseWebhook(String signature, String payload) {
        Map<String, Object> data = toPayloadObject(payload);
        return new PaymentWebhookEvent(
            String.valueOf(data.getOrDefault("providerPaymentId", "")),
            String.valueOf(data.getOrDefault("providerOrderId", "")),
            String.valueOf(data.getOrDefault("status", "FAILED")),
            Integer.parseInt(String.valueOf(data.getOrDefault("amount", 0))),
            String.valueOf(data.getOrDefault("currency", "INR")),
            String.valueOf(data.getOrDefault("ownerId", ""))
        );
    }

    @Override
    public boolean validateWebhookSignature(String signature, String payload) {
        try {
            String webhookSecret = appProperties.getRazorpay().getWebhookSecret();
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equalsIgnoreCase(signature);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String refund(String providerPaymentId, String reason) {
        // Integration point: replace with Razorpay refund API call.
        return "refund_" + UUID.randomUUID();
    }

    @Override
    public String providerName() {
        return "RAZORPAY";
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
