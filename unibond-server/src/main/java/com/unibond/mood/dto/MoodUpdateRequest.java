package com.unibond.mood.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record MoodUpdateRequest(
    @NotBlank @Size(max = 10) String emoji,
    @Size(max = 50) String text) {}
