package com.carpool.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyLoginOtpResponse {
    private boolean success;
    private String token;
    private String refreshToken;
    private java.util.UUID userId;
    private com.carpool.entity.Role role;
    private String mobile;
    private String message;
}
