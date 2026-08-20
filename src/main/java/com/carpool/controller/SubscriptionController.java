package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.subscription.CreateCheckoutRequest;
import com.carpool.dto.subscription.RefundRequest;
import com.carpool.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final com.carpool.repository.SubscriptionPlanRepository planRepository;

    @PostMapping("/create-checkout")
    public ApiResponse<?> createCheckout(@RequestBody CreateCheckoutRequest request) {
        return ApiResponse.of(subscriptionService.createCheckout(request));
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<?> webhook(@RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
                                  @RequestBody String payload) {
        subscriptionService.processWebhook(signature == null ? "" : signature, payload);
        return ApiResponse.of(java.util.Map.of("status", "processed"));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.of(subscriptionService.mySubscriptions());
    }

    @GetMapping("/plans")
    public ApiResponse<?> plans() {
        var plans = planRepository.findByActiveTrueOrderByCreatedAtAsc();
        return ApiResponse.of(plans);
    }

    @GetMapping("/admin")
    public ApiResponse<?> adminList(@RequestParam(required = false) String status) {
        return ApiResponse.of(subscriptionService.adminList(status));
    }

    @PostMapping("/{subscriptionId}/approve")
    public ApiResponse<?> approve(@PathVariable UUID subscriptionId) {
        return ApiResponse.of(subscriptionService.approve(subscriptionId));
    }

    @PostMapping("/{subscriptionId}/reject")
    public ApiResponse<?> reject(@PathVariable UUID subscriptionId, @RequestParam String comment) {
        return ApiResponse.of(subscriptionService.reject(subscriptionId, comment));
    }

    @GetMapping(value = "/admin/export", produces = "text/csv")
    public ResponseEntity<String> export() {
        var rows = subscriptionService.adminList(null);
        StringBuilder csv = new StringBuilder("Owner Name,Owner Mobile,Date of Birth,Payment Date,Start Date,End Date,Amount,UTR,Status\n");
        rows.forEach(s -> csv.append(csv(s.getOwnerName())).append(',').append(csv(s.getOwnerMobile())).append(',')
            .append(csv(String.valueOf(s.getOwnerDateOfBirth()))).append(',').append(csv(String.valueOf(s.getPaymentDate()))).append(',')
            .append(csv(String.valueOf(s.getStartsAt()))).append(',').append(csv(String.valueOf(s.getExpiresAt()))).append(',')
            .append(s.getAmount() / 100.0).append(',').append(csv(s.getUtrNumber())).append(',').append(s.getStatus()).append('\n'));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=subscription-audit.csv").body(csv.toString());
    }

    private String csv(String value) {
        String safe = value == null || "null".equals(value) ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    @PostMapping("/{subscriptionId}/utr")
    public ApiResponse<?> submitUtr(@PathVariable UUID subscriptionId, @RequestParam String utrNumber) {
        return ApiResponse.of(subscriptionService.submitUtr(subscriptionId, utrNumber));
    }

    @PostMapping("/{subscriptionId}/refund")
    public ApiResponse<?> refund(@PathVariable UUID subscriptionId, @RequestBody(required = false) RefundRequest request) {
        return ApiResponse.of(subscriptionService.refund(subscriptionId, request == null ? null : request.getReason()));
    }
}
