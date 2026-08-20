package com.carpool.service.payment;

public record PaymentWebhookEvent(String paymentId, String orderId, String status, int amount, String currency, String ownerId) {
}
