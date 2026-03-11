package com.bukubesarkami.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Berhasil", data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, OffsetDateTime.now());
    }
}