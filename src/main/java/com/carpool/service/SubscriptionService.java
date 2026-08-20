package com.carpool.service;

import com.carpool.dto.subscription.CheckoutResponse;
import com.carpool.dto.subscription.CreateCheckoutRequest;
import com.carpool.dto.subscription.SubscriptionResponse;
import com.carpool.entity.NotificationType;
import com.carpool.entity.OwnerProfile;
import com.carpool.entity.PaymentProvider;
import com.carpool.entity.Subscription;
import com.carpool.entity.SubscriptionStatus;
import com.carpool.entity.Role;
import com.carpool.entity.VerificationStatus;
import com.carpool.exception.AppException;
import com.carpool.repository.KycDocumentRepository;
import com.carpool.repository.OwnerProfileRepository;
import com.carpool.repository.SubscriptionRepository;
import com.carpool.repository.SubscriptionPlanRepository;
import com.carpool.security.AuthFacade;
import com.carpool.service.payment.PaymentCheckout;
import com.carpool.service.payment.PaymentProviderClient;
import com.carpool.service.payment.PaymentWebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final OwnerProfileRepository ownerRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final PaymentProviderClient paymentProviderClient;
    private final AuthFacade authFacade;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public CheckoutResponse createCheckout(CreateCheckoutRequest request) {
        expireDueSubscriptions();
        var principal = authFacade.currentUser();
        if (principal.getOwnerId() == null) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Owner profile required");
        }
        OwnerProfile owner = ownerRepository.findById(principal.getOwnerId()).orElseThrow();
        if (request.getPlanId() == null || request.getPlanId().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "planId is required");
        }
        var plan = planRepository.findById(java.util.UUID.fromString(request.getPlanId()))
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Subscription plan not found"));

        Subscription sub = new Subscription();
        sub.setOwner(owner);
        sub.setPlan(plan);
        sub.setAmount(plan.getAmountPaise());
        sub.setCurrency(plan.getCurrency());
        sub.setStatus(SubscriptionStatus.CREATED);
        sub.setProvider(PaymentProvider.valueOf(paymentProviderClient.providerName()));
        sub = subscriptionRepository.save(sub);
        sub.setStatus(SubscriptionStatus.PENDING);
        subscriptionRepository.save(sub);

        var checkout = paymentProviderClient.createCheckout(sub.getId().toString(), plan.getAmountPaise(), plan.getCurrency(), request.getSuccessUrl(), request.getCancelUrl(), owner.getId().toString());
        sub.setProviderPaymentId(checkout.orderId());
        subscriptionRepository.save(sub);

        return CheckoutResponse.builder()
            .subscriptionId(sub.getId())
            .amount(plan.getAmountPaise())
            .currency(plan.getCurrency())
            .provider(paymentProviderClient.providerName())
            .checkoutUrl(checkout.checkoutUrl())
            .providerOrderId(checkout.orderId())
            .build();
    }

    @Transactional
    public void processWebhook(String signature, String payload) {
        if (!paymentProviderClient.validateWebhookSignature(signature, payload)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid webhook signature");
        }
        PaymentWebhookEvent event = paymentProviderClient.parseWebhook(signature, payload);

        Subscription sub = subscriptionRepository.findByProviderPaymentId(event.orderId())
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Subscription not found"));

        if (sub.getStatus() == SubscriptionStatus.PAID || sub.getStatus() == SubscriptionStatus.REFUNDED) {
            return;
        }

        if ("PAID".equalsIgnoreCase(event.status()) || "SUCCESS".equalsIgnoreCase(event.status())) {
            sub.setStatus(SubscriptionStatus.PAID);
            sub.setProviderPaymentId(event.paymentId());
            sub.setStartsAt(Instant.now());
            if (sub.getPlan() != null && sub.getPlan().getDurationMonths() > 0) {
                sub.setExpiresAt(Instant.now().atZone(ZoneOffset.UTC).plusMonths(sub.getPlan().getDurationMonths()).toInstant());
            } else {
                sub.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
            }
            notificationService.create(sub.getOwner().getUser().getId(), NotificationType.SUBSCRIPTION_PAYMENT_SUCCESS,
                "Subscription active", "Your subscription payment was successful.");
        } else {
            sub.setStatus(SubscriptionStatus.FAILED);
            notificationService.create(sub.getOwner().getUser().getId(), NotificationType.SUBSCRIPTION_PAYMENT_FAILURE,
                "Subscription failed", "Your subscription payment failed.");
        }

        subscriptionRepository.save(sub);
        auditService.log("SUBSCRIPTION_WEBHOOK", "system", sub.getId().toString(), "{\"status\":\"" + sub.getStatus() + "\"}");
    }

    public List<SubscriptionResponse> mySubscriptions() {
        var principal = authFacade.currentUser();
        if (principal.getOwnerId() == null) {
            return List.of();
        }
        return subscriptionRepository.findByOwnerIdOrderByCreatedAtDesc(principal.getOwnerId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public SubscriptionResponse submitUtr(UUID subscriptionId, String utrNumber) {
        var principal = authFacade.currentUser();
        Subscription sub = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Subscription not found"));
        if (principal.getOwnerId() == null || !sub.getOwner().getId().equals(principal.getOwnerId())) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not your subscription");
        }
        if (utrNumber == null || utrNumber.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "UTR number is required");
        }
        if (sub.getStatus() != SubscriptionStatus.PENDING && sub.getStatus() != SubscriptionStatus.CREATED) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Payment is already submitted");
        }
        sub.setUtrNumber(utrNumber.trim());
        sub.setPaymentDate(Instant.now());
        sub.setStatus(SubscriptionStatus.VERIFICATION_IN_PROGRESS);
        subscriptionRepository.save(sub);
        return toResponse(sub);
    }

    @Transactional
    public SubscriptionResponse refund(UUID subscriptionId, String reason) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Subscription not found"));
        if (sub.getStatus() != SubscriptionStatus.PAID) {
            throw new AppException(HttpStatus.CONFLICT, "CONFLICT", "Only paid subscriptions can be refunded");
        }
        paymentProviderClient.refund(sub.getProviderPaymentId(), reason == null ? "n/a" : reason);
        sub.setStatus(SubscriptionStatus.REFUNDED);
        OwnerProfile owner = sub.getOwner();
        owner.setVerified(false);
        owner.setVerificationStatus(VerificationStatus.PENDING);
        ownerRepository.save(owner);
        subscriptionRepository.save(sub);
        auditService.log("SUBSCRIPTION_REFUND", authFacade.currentUser().getUserId().toString(), sub.getId().toString(), "{}");
        return toResponse(sub);
    }

    public List<SubscriptionResponse> adminList(String status) {
        requireAdmin();
        expireDueSubscriptions();
        SubscriptionStatus filter = status == null || status.isBlank() ? null : SubscriptionStatus.valueOf(status.toUpperCase());
        return subscriptionRepository.findAll().stream()
            .filter(s -> filter == null || s.getStatus() == filter)
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(this::toResponse).toList();
    }

    @Transactional
    public SubscriptionResponse approve(UUID subscriptionId) {
        requireAdmin();
        Subscription sub = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Subscription not found"));
        if (sub.getStatus() != SubscriptionStatus.VERIFICATION_IN_PROGRESS) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Only submitted payments can be approved");
        }
        var plan = sub.getPlan();
        if (plan == null) {
            plan = planRepository.findFirstByActiveTrueOrderByCreatedAtAsc()
                .orElseThrow(() -> new AppException(HttpStatus.SERVICE_UNAVAILABLE, "PLAN_UNAVAILABLE", "Subscription plan unavailable"));
        }
        Instant approvedAt = Instant.now();
        sub.setStatus(SubscriptionStatus.PAID);
        sub.setReviewedAt(approvedAt);
        sub.setStartsAt(approvedAt);
        sub.setExpiresAt(approvedAt.atZone(ZoneOffset.UTC).plusMonths(plan.getDurationMonths()).toInstant());
        sub.setRejectionComment(null);
        sub.getOwner().setVerified(true);
        sub.getOwner().setVerificationStatus(VerificationStatus.VERIFIED);
        ownerRepository.save(sub.getOwner());
        subscriptionRepository.save(sub);
        notificationService.create(sub.getOwner().getUser().getId(), NotificationType.SUBSCRIPTION_PAYMENT_SUCCESS,
            "Subscription approved", "Your subscription was approved and is active.");
        auditService.log("SUBSCRIPTION_APPROVED", authFacade.currentUser().getUserId().toString(), subscriptionId.toString(), "{}");
        return toResponse(sub);
    }

    @Transactional
    public SubscriptionResponse reject(UUID subscriptionId, String comment) {
        requireAdmin();
        if (comment == null || comment.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Rejection comment is required");
        }
        Subscription sub = subscriptionRepository.findById(subscriptionId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Subscription not found"));
        if (sub.getStatus() != SubscriptionStatus.VERIFICATION_IN_PROGRESS) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "Only submitted payments can be rejected");
        }
        sub.setStatus(SubscriptionStatus.REJECTED);
        sub.setReviewedAt(Instant.now());
        sub.setRejectionComment(comment.trim());
        sub.getOwner().setVerified(false);
        sub.getOwner().setVerificationStatus(VerificationStatus.PENDING);
        ownerRepository.save(sub.getOwner());
        subscriptionRepository.save(sub);
        notificationService.create(sub.getOwner().getUser().getId(), NotificationType.SUBSCRIPTION_PAYMENT_FAILURE,
            "Subscription rejected", comment.trim());
        auditService.log("SUBSCRIPTION_REJECTED", authFacade.currentUser().getUserId().toString(), subscriptionId.toString(), "{}");
        return toResponse(sub);
    }

    @Transactional
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3600000)
    public void expireDueSubscriptions() {
        Instant now = Instant.now();
        subscriptionRepository.findAll().stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.PAID && s.getExpiresAt() != null && s.getExpiresAt().isBefore(now))
            .forEach(s -> {
                s.setStatus(SubscriptionStatus.INACTIVE);
                s.getOwner().setVerified(false);
                s.getOwner().setVerificationStatus(VerificationStatus.PENDING);
                ownerRepository.save(s.getOwner());
                subscriptionRepository.save(s);
            });
    }

    private void requireAdmin() {
        if (authFacade.currentUser().getRole() != Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin access required");
        }
    }

    private SubscriptionResponse toResponse(Subscription s) {
        return SubscriptionResponse.builder()
            .id(s.getId())
            .ownerId(s.getOwner().getId())
            .ownerName(s.getOwner().getName())
            .ownerMobile(s.getOwner().getMobile())
            .ownerDateOfBirth(s.getOwner().getUser().getDateOfBirth())
            .ownerAge(s.getOwner().getUser().getAge())
            .amount(s.getAmount())
            .currency(s.getCurrency())
            .provider(s.getProvider())
            .providerPaymentId(s.getProviderPaymentId())
            .utrNumber(s.getUtrNumber())
            .paymentDate(s.getPaymentDate())
            .reviewedAt(s.getReviewedAt())
            .rejectionComment(s.getRejectionComment())
            .status(s.getStatus())
            .startsAt(s.getStartsAt())
            .expiresAt(s.getExpiresAt())
            .planName(s.getPlan() == null ? null : s.getPlan().getName())
            .planId(s.getPlan() == null ? null : s.getPlan().getId())
            .createdAt(s.getCreatedAt())
            .updatedAt(s.getUpdatedAt())
            .build();
    }
}
