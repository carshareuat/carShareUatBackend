package com.carpool.service.payment;

public record PaymentCheckout(String checkoutUrl, String orderId, String provider) {
}
