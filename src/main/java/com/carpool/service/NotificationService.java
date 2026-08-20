package com.carpool.service;

import com.carpool.dto.notification.NotificationResponse;
import com.carpool.entity.Notification;
import com.carpool.entity.NotificationType;
import com.carpool.entity.User;
import com.carpool.exception.AppException;
import com.carpool.repository.NotificationRepository;
import com.carpool.repository.UserRepository;
import com.carpool.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuthFacade authFacade;
    private final com.carpool.repository.PushSubscriptionRepository pushSubscriptionRepository;
    private final com.carpool.service.PushSenderService pushSenderService;
    private final PushNotificationService pushNotificationService;
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Transactional
    public void create(UUID userId, NotificationType type, String title, String body) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notificationRepository.save(notification);
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("title", title);
            payload.put("body", body);
            payload.put("url", "/");
            log.info("Creating notification for user={} title={} body={}", userId, title, body);
            pushSenderService.sendToUser(userId, payload);
            pushNotificationService.sendNotification(
                com.carpool.dto.push.PushNotificationRequest.builder()
                    .userId(userId)
                    .title(title)
                    .body(body)
                    .route("/")
                    .build()
            );
            log.debug("Called both legacy web push and FCM push for user={}", userId);
        } catch (Exception e) {
            log.warn("Failed to send push for user={} -> {}", userId, e.getMessage());
            log.debug("NotificationService exception", e);
        }
    }

    public List<NotificationResponse> myNotifications() {
        UUID userId = authFacade.currentUser().getUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void markRead(UUID notificationId) {
        UUID userId = authFacade.currentUser().getUserId();
        Notification n = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead() {
        UUID userId = authFacade.currentUser().getUserId();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .type(n.getType())
            .title(n.getTitle())
            .body(n.getBody())
            .read(n.isRead())
            .createdAt(n.getCreatedAt())
            .build();
    }
}
