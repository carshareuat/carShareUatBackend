package com.carpool.controller;

import com.carpool.service.PushSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/push")
@RequiredArgsConstructor
public class TestPushController {
    private final PushSenderService pushSenderService;

    @PostMapping("/test")
    public ResponseEntity<?> sendTest(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> p = payload == null || payload.isEmpty() ? Map.of("title", "Test", "body", "This is a test push", "url", "/") : payload;
        pushSenderService.sendToAll(p);
        return ResponseEntity.ok(Map.of("status", "sent", "recipients", "all"));
    }
}
