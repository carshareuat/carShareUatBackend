package com.carpool.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginOtpRequest {
    @NotBlank
    private String mobileNumber;
}
