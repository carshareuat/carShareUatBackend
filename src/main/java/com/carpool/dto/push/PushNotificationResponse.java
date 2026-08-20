package com.carpool.dto.push;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationResponse {
    private boolean success;
    private String message;
    private int totalTokens;
    private int sent;
    private int failed;
    private List<String> errors;
}
