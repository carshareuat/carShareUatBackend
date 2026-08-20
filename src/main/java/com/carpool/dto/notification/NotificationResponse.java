package com.carpool.dto.notification;

import com.carpool.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String body;
    private boolean read;
    private Instant createdAt;
}
