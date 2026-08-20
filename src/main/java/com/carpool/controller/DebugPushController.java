package com.carpool.controller;

import com.carpool.service.PushSenderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.io.PrintWriter;
import java.io.StringWriter;

@RestController
@RequestMapping("/api/debug/push")
@RequiredArgsConstructor
public class DebugPushController {
    private final PushSenderService pushSenderService;
    private static final Logger log = LoggerFactory.getLogger(DebugPushController.class);

    @PostMapping("/test")
    public ResponseEntity<?> sendTest(@RequestBody(required = false) Map<String, Object> payload) {
        Map<String, Object> p = payload == null || payload.isEmpty() ? Map.of("title", "Debug Test", "body", "Java push test", "url", "/") : payload;
        // Ensure there is an obvious stdout log for container logs
        try {
            System.out.println("[DebugPushController] Invoked debug push with payload: " + p);
        } catch (Exception e) { /* ignore */ }
        try {
            com.carpool.service.PushSenderService.PushResult result = pushSenderService.sendToAll(p);
            try { System.out.println("[DebugPushController] sendToAll result: total=" + result.totalSubscriptions + " sent=" + result.sent + " failed=" + result.failed + " expiredRemoved=" + result.expiredRemoved + " errors=" + result.errors); } catch (Exception e) {}
            return ResponseEntity.ok(Map.of(
                "status", result.sent > 0 ? "sent" : "no_subscriptions_or_failed",
                "totalSubscriptions", result.totalSubscriptions,
                "sent", result.sent,
                "failed", result.failed,
                "expiredRemoved", result.expiredRemoved,
                "errors", result.errors
            ));
        } catch (Throwable e) {
            // Ensure full stacktrace is printed to stdout/stderr and logs so we can diagnose the 500
            try { System.err.println("[DebugPushController] Exception during sendToAll"); e.printStackTrace(); } catch (Exception ignore) {}
            log.error("Debug push failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage()));
        }
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<?> listSubscriptions() {
        try {
            var subs = pushSenderService.getAllSubscriptions();
            return ResponseEntity.ok(Map.of("count", subs.size(), "subscriptions", subs));
        } catch (Throwable e) {
            try { System.err.println("[DebugPushController] listSubscriptions error"); e.printStackTrace(); } catch (Exception ignore) {}
            StringWriter sw = new StringWriter(); e.printStackTrace(new PrintWriter(sw));
            return ResponseEntity.status(500).body(Map.of("error", "INTERNAL_ERROR", "message", e.getMessage(), "trace", sw.toString()));
        }
    }

    @PostMapping("/subscriptions/clean")
    public ResponseEntity<?> cleanInvalidSubscriptions() {
        int removed = pushSenderService.removeInvalidSubscriptions();
        return ResponseEntity.ok(Map.of("removed", removed));
    }

    @PostMapping("/subscriptions/normalize")
    public ResponseEntity<?> normalizeSubscriptions() {
        int updated = pushSenderService.normalizeSubscriptions();
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<?> deleteSubscription(@org.springframework.web.bind.annotation.PathVariable("id") String id) {
        try {
            pushSenderService.deleteSubscriptionById(id);
            return ResponseEntity.ok(Map.of("deleted", id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
