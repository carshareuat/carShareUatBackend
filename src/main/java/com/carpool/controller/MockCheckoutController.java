package com.carpool.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockCheckoutController {

    @GetMapping(value = "/mock-checkout", produces = MediaType.TEXT_HTML_VALUE)
    public String checkoutPage(@RequestParam String subscriptionId) {
        return """
            <!doctype html>
            <html>
              <head>
                <meta charset=\"utf-8\" />
                <title>Mock Checkout</title>
              </head>
              <body style=\"font-family: Arial, sans-serif; padding: 24px;\">
                <h2>Mock Payment Checkout</h2>
                <p>Subscription ID: <b>%s</b></p>
                <p>This is a mock local payment page for development.</p>
                <p>Use <code>/api/subscriptions/webhook</code> to simulate payment status updates.</p>
              </body>
            </html>
            """.formatted(subscriptionId);
    }
}
