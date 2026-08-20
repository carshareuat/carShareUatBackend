package com.carpool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private PageMeta meta;

    public static <T> ApiResponse<T> of(T data) {
        return ApiResponse.<T>builder().data(data).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().data(data).build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder().data((T) java.util.Map.of("message", message)).build();
    }

    @SuppressWarnings("unchecked")
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .data((T) java.util.Map.of("message", message))
                .build();
    }

    public static ApiResponse<java.util.Map<String, Object>> ofStatus(int code, String message) {
        return ApiResponse.of(java.util.Map.of("status", code, "message", message));
    }
}
