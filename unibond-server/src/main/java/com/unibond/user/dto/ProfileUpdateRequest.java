package com.unibond.user.dto;
import jakarta.validation.constraints.Size;
public record ProfileUpdateRequest(
    @Size(max = 20) String nickname,
    @Size(max = 500) String avatarUrl) {}
