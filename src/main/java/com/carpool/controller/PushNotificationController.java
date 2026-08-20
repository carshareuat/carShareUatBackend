package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.push.DeviceTokenRequest;
import com.carpool.dto.push.PushNotificationRequest;
import com.carpool.exception.AppException;
import com.carpool.security.AuthFacade;
import com.carpool.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushNotificationController {

    private final PushNotificationService pushNotificationService;
    private final AuthFacade authFacade;

    @PostMapping("/register-token")
    public ApiResponse<?> registerToken(@Valid @RequestBody DeviceTokenRequest request) {
        UUID currentUserId = authFacade.currentUser().getUserId();
        if (request.getUserId() != null && !request.getUserId().equals(currentUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "USER_ID_MISMATCH", "You can only register your own device token");
        }
        request.setUserId(currentUserId);
        return ApiResponse.of(pushNotificationService.saveToken(request));
    }

    @PostMapping("/send")
    public ApiResponse<?> send(@Valid @RequestBody PushNotificationRequest request) {
        UUID currentUserId = authFacade.currentUser().getUserId();
        if (request.getUserId() != null && !request.getUserId().equals(currentUserId)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "USER_ID_MISMATCH", "You can only send notifications to your own devices");
        }
        request.setUserId(currentUserId);
        return ApiResponse.of(pushNotificationService.sendNotification(request));
    }

    @DeleteMapping("/remove-token")
    public ApiResponse<?> removeToken(@RequestParam String token) {
        UUID currentUserId = authFacade.currentUser().getUserId();
        pushNotificationService.removeToken(currentUserId, token);
        return ApiResponse.of(Map.of("status", "ok", "message", "Token removed"));
    }

    @GetMapping("/test")
    public ApiResponse<?> test() {
        return ApiResponse.of(Map.of(
            "status", "ok",
            "message", "Push notification API is active",
            "userId", authFacade.currentUser().getUserId().toString()
        ));
    }
}
