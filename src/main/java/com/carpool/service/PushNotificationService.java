package com.carpool.service;

import com.carpool.dto.push.DeviceTokenRequest;
import com.carpool.dto.push.DeviceTokenResponse;
import com.carpool.dto.push.PushNotificationRequest;
import com.carpool.dto.push.PushNotificationResponse;
import com.carpool.entity.DeviceToken;
import com.carpool.entity.User;
import com.carpool.exception.AppException;
import com.carpool.repository.DeviceTokenRepository;
import com.carpool.repository.UserRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeviceTokenResponse saveToken(DeviceTokenRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "FCM token is required");
        }

        if (request.getUserId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_USER", "User id is required");
        }

        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String token = request.getToken().trim();
        String deviceType = Optional.ofNullable(request.getDeviceType()).map(String::trim).filter(v -> !v.isBlank()).orElse("web");

        Optional<DeviceToken> existing = deviceTokenRepository.findFirstByUserIdAndFcmTokenOrderByUpdatedAtDesc(user.getId(), token);
        if (existing.isPresent()) {
            DeviceToken deviceToken = existing.get();
            if (!deviceType.equals(deviceToken.getDeviceType())) {
                deviceToken.setDeviceType(deviceType);
            }
            deviceToken.setUpdatedAt(Instant.now());
            return mapToResponse(deviceTokenRepository.save(deviceToken));
        }

        DeviceToken deviceToken = new DeviceToken();
        deviceToken.setUserId(user.getId());
        deviceToken.setFcmToken(token);
        deviceToken.setDeviceType(deviceType);

        try {
            return mapToResponse(deviceTokenRepository.save(deviceToken));
        } catch (DataIntegrityViolationException e) {
            Optional<DeviceToken> duplicate = deviceTokenRepository.findFirstByUserIdAndFcmTokenOrderByUpdatedAtDesc(user.getId(), token);
            if (duplicate.isPresent()) {
                DeviceToken existingToken = duplicate.get();
                if (!deviceType.equals(existingToken.getDeviceType())) {
                    existingToken.setDeviceType(deviceType);
                    existingToken.setUpdatedAt(Instant.now());
                    return mapToResponse(deviceTokenRepository.save(existingToken));
                }
                return mapToResponse(existingToken);
            }
            throw e;
        }
    }

    @Transactional
    public void removeToken(UUID userId, String token) {
        if (userId == null || token == null || token.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "User id and token are required");
        }
        deviceTokenRepository.deleteByUserIdAndToken(userId, token.trim());
    }

    @Transactional
    public void removeTokenForUser(UUID userId) {
        if (userId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_USER", "User id is required");
        }
        deviceTokenRepository.deleteAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getTokensForUser(UUID userId) {
        if (userId == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_USER", "User id is required");
        }
        return deviceTokenRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    public PushNotificationResponse sendNotification(PushNotificationRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "User id is required");
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(request.getUserId());
        if (tokens.isEmpty()) {
            return PushNotificationResponse.builder()
                .success(false)
                .message("No device tokens registered for this user")
                .totalTokens(0)
                .sent(0)
                .failed(0)
                .errors(List.of("No device tokens registered for this user"))
                .build();
        }

        List<String> fcmTokens = tokens.stream()
            .map(DeviceToken::getFcmToken)
            .filter(token -> token != null && !token.isBlank())
            .toList();

        if (fcmTokens.isEmpty()) {
            return PushNotificationResponse.builder()
                .success(false)
                .message("No valid FCM tokens found")
                .totalTokens(0)
                .sent(0)
                .failed(0)
                .errors(List.of("No valid FCM tokens found"))
                .build();
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                return PushNotificationResponse.builder()
                    .success(false)
                    .message("Firebase is not initialized. Configure service-account JSON or GOOGLE_APPLICATION_CREDENTIALS.")
                    .totalTokens(fcmTokens.size())
                    .sent(0)
                    .failed(fcmTokens.size())
                    .errors(List.of("Firebase is not initialized"))
                    .build();
            }

            FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
            List<String> failedTokens = new ArrayList<>();

            MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                    .setTitle(request.getTitle())
                    .setBody(request.getBody())
                    .setImage(request.getImageUrl())
                    .build())
                .putData("title", request.getTitle() == null ? "" : request.getTitle())
                .putData("body", request.getBody() == null ? "" : request.getBody())
                .putData("route", request.getRoute() == null ? "/" : request.getRoute())
                .putData("source", "carshare-backend")
                .addAllTokens(fcmTokens)
                .build();

            var response = firebaseMessaging.sendEachForMulticast(message);
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        failedTokens.add(fcmTokens.get(i));
                    }
                }
            }

            return PushNotificationResponse.builder()
                .success(response.getFailureCount() == 0)
                .message("Notification sent to " + response.getSuccessCount() + " devices")
                .totalTokens(fcmTokens.size())
                .sent(response.getSuccessCount())
                .failed(response.getFailureCount())
                .errors(failedTokens.isEmpty() ? List.of() : failedTokens)
                .build();

        } catch (Exception e) {
            log.error("FCM send failed for user {}", request.getUserId(), e);
            return PushNotificationResponse.builder()
                .success(false)
                .message("Failed to send push notification")
                .totalTokens(fcmTokens.size())
                .sent(0)
                .failed(fcmTokens.size())
                .errors(List.of(e.getMessage()))
                .build();
        }
    }

    public void sendNotificationToToken(String token, String title, String body, String route) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("title", title == null ? "" : title)
                .putData("body", body == null ? "" : body)
                .putData("route", route == null ? "/" : route)
                .putData("source", "carshare-backend")
                .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("Failed to send notification to single token: {}", token, e);
        }
    }

    public void sendMulticastNotification(List<String> tokens, String title, String body, String route) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        try {
            MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("title", title == null ? "" : title)
                .putData("body", body == null ? "" : body)
                .putData("route", route == null ? "/" : route)
                .putData("source", "carshare-backend")
                .addAllTokens(tokens)
                .build();
            FirebaseMessaging.getInstance().sendEachForMulticast(message);
        } catch (Exception e) {
            log.warn("Failed to send multicast push", e);
        }
    }

    public void cleanupInvalidTokens() {
        List<DeviceToken> tokens = deviceTokenRepository.findAll();
        for (DeviceToken token : tokens) {
            if (token.getFcmToken() == null || token.getFcmToken().isBlank()) {
                deviceTokenRepository.delete(token);
            }
        }
    }

    public static FirebaseApp initializeFirebaseApp(String serviceAccountJson) {
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "FIREBASE_CONFIG_MISSING", "Firebase service account configuration is missing");
        }

        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        } catch (Exception e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "FIREBASE_INIT_FAILED", "Firebase initialization failed: " + e.getMessage(), e);
        }
    }

    private DeviceTokenResponse mapToResponse(DeviceToken deviceToken) {
        if (deviceToken == null) {
            return null;
        }

        return DeviceTokenResponse.builder()
            .id(deviceToken.getId())
            .userId(deviceToken.getUserId())
            .token(deviceToken.getFcmToken())
            .deviceType(deviceToken.getDeviceType())
            .createdDate(deviceToken.getCreatedAt())
            .updatedDate(deviceToken.getUpdatedAt())
            .active(true)
            .build();
    }
}
