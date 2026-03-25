package com.unibond.common.dto;

public record ApiResponse<T>(T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data);
    }
}
