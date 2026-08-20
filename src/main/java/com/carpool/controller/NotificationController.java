package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.of(notificationService.myNotifications());
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<?> markRead(@PathVariable UUID notificationId) {
        notificationService.markRead(notificationId);
        return ApiResponse.of(java.util.Map.of("status", "ok"));
    }

    @PatchMapping("/read-all")
    public ApiResponse<?> markAllRead() {
        notificationService.markAllRead();
        return ApiResponse.of(java.util.Map.of("status", "ok"));
    }
}
