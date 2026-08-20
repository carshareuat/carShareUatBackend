package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.entity.PushSubscription;
import com.carpool.repository.PushSubscriptionRepository;
import com.carpool.repository.UserRepository;
import com.carpool.entity.User;
import com.carpool.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/push-subscriptions")
@RequiredArgsConstructor
public class PushSubscriptionController {
    private final PushSubscriptionRepository repo;
    private final AuthFacade authFacade;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionController.class);

    @PostMapping("/push")
    public ApiResponse<?> save(@RequestBody java.util.Map<String, Object> body) {
        var principal = authFacade.currentUser();
        java.util.UUID userId = principal.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String endpoint = (String) body.getOrDefault("endpoint", "");
        // Upsert by endpoint: the same browser subscription may be re-sent on every login/navigation,
        // so avoid creating duplicate rows that would trigger duplicate push notifications.
        var sub = repo.findByEndpoint(endpoint).orElseGet(PushSubscription::new);
        sub.setUser(user);
        sub.setEndpoint(endpoint);
        var keys = (java.util.Map<String, String>) body.getOrDefault("keys", java.util.Map.of());
        sub.setP256dh(keys.getOrDefault("p256dh", ""));
        sub.setAuth(keys.getOrDefault("auth", ""));
        try {
            repo.save(sub);
            // Use logger instead of System.out to avoid excessive stdout logs
            try { log.info("Saved subscription for user={} endpoint={}", userId, sub.getEndpoint()); } catch (Exception e) { /* ignore */ }
        } catch (Exception e) {
            log.error("Failed to save subscription for user={}", userId, e);
            throw e;
        }
        return ApiResponse.of(java.util.Map.of("status", "ok"));
    }

    @GetMapping("/me")
    public ApiResponse<List<PushSubscription>> my() {
        java.util.UUID userId = authFacade.currentUser().getUserId();
        return ApiResponse.of(repo.findByUserId(userId));
    }

    @GetMapping("/admin")
    public ApiResponse<List<PushSubscription>> listAll() { return ApiResponse.of(repo.findAll()); }
}
