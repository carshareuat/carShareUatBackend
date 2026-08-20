package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.auth.LoginOtpRequest;
import com.carpool.dto.auth.LoginOtpResponse;
import com.carpool.dto.auth.VerifyLoginOtpRequest;
import com.carpool.dto.auth.VerifyLoginOtpResponse;
import com.carpool.service.OtpLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class OtpLoginController {

    private final OtpLoginService otpLoginService;

    @PostMapping("/check-mobile")
    public ResponseEntity<ApiResponse<LoginOtpResponse>> checkMobile(@Valid @RequestBody LoginOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.of(otpLoginService.checkMobile(request)));
    }

    @PostMapping("/send-login-otp")
    public ResponseEntity<ApiResponse<Void>> sendLoginOtp(@Valid @RequestBody LoginOtpRequest request) {
        otpLoginService.sendLoginOtp(request);
        return ResponseEntity.ok(ApiResponse.of(null));
    }

    @PostMapping("/verify-login-otp")
    public ResponseEntity<ApiResponse<VerifyLoginOtpResponse>> verifyLoginOtp(@Valid @RequestBody VerifyLoginOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.of(otpLoginService.verifyLoginOtp(request)));
    }
}
