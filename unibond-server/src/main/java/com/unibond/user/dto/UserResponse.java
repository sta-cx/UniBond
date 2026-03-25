package com.unibond.user.dto;
import com.unibond.user.entity.AuthProvider;
import java.time.Instant;
public record UserResponse(Long id, String email, String nickname, String avatarUrl,
    AuthProvider authProvider, String inviteCode, Long partnerId, Instant createdAt) {}
