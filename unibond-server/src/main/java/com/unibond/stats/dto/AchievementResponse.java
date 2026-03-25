package com.unibond.stats.dto;
import java.time.Instant;
public record AchievementResponse(String type, String displayName,
    boolean unlocked, Instant unlockedAt) {}
