package com.unibond.auth.dto;
public record AuthResponse(String accessToken, String refreshToken, Long userId, boolean isNew) {}
