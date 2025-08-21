package com.greencloud.server.dto.responses;

import java.time.Instant;

public class APIResponse<T> {
    private boolean success;   // 성공 여부
    private String message;    // 설명/에러 메시지
    private T data;            // 성공 시 응답 데이터 (없으면 null)
    private Instant timestamp; // 응답 시각 (옵션)

    public APIResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    // 성공
    public static <T> APIResponse<T> success(String message) {
        return new APIResponse<>(true, message, null);
    }

    // 성공 + 데이터
    public static <T> APIResponse<T> success(String message, T data) {
        return new APIResponse<>(true, message, data);
    }

    // 실패 + 메시지
    public static <T> APIResponse<T> fail(String message) {
        return new APIResponse<>(false, message, null);
    }

    // 실패 + 메시지 + 데이터
    public static <T> APIResponse<T> fail(String message, T data) {
        return new APIResponse<>(false, message, data);
    }

    // Getters/Setters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
    public Instant getTimestamp() { return timestamp; }
}