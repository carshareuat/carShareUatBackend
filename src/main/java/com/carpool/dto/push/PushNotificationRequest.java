package com.carpool.dto.push;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationRequest {
    private UUID userId;
    private String title;
    private String body;
    private String route;
    private String imageUrl;
}
