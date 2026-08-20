package com.carpool.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyLoginOtpRequest {
    @NotBlank
    private String mobileNumber;

    @NotBlank
    private String otp;
}
