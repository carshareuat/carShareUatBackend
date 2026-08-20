package com.carpool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private ErrorPayload error;
    private Instant timestamp;
    private String path;

    @Data
    @Builder
    @AllArgsConstructor
    public static class ErrorPayload {
        private String code;
        private String message;
        private Map<String, Object> details;
    }
}
