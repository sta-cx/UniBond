package com.unibond.mood.dto;
import java.time.Instant;
public record MoodResponse(String emoji, String text, Instant updatedAt) {}
