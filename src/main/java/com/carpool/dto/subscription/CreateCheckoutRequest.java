package com.carpool.dto.subscription;

import lombok.Data;

@Data
public class CreateCheckoutRequest {
    private String successUrl;
    private String cancelUrl;
    private String planId;
}
