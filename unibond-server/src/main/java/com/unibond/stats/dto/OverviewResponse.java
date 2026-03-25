package com.unibond.stats.dto;
import java.util.List;
public record OverviewResponse(int todayScore, int streakDays, int totalQuizzes,
    double avgScore, List<AchievementResponse> recentAchievements) {}
