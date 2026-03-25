package com.unibond.stats.service;

import com.unibond.stats.dto.*;
import com.unibond.stats.entity.Achievement;
import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.entity.DailyStats;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class StatsService {
    private final DailyStatsRepository statsRepo;
    private final AchievementRepository achievementRepo;

    public StatsService(DailyStatsRepository statsRepo, AchievementRepository achievementRepo) {
        this.statsRepo = statsRepo;
        this.achievementRepo = achievementRepo;
    }

    public OverviewResponse getOverview(Long coupleId) {
        LocalDate today = LocalDate.now();
        var todayStats = statsRepo.findById(new com.unibond.stats.entity.DailyStatsId(coupleId, today));
        int todayScore = todayStats.map(DailyStats::getMatchScore).orElse(0);
        int streakDays = todayStats.map(DailyStats::getStreakDays).orElse(0);

        LocalDate weekAgo = today.minusDays(30);
        List<DailyStats> recentStats = statsRepo.findByCoupleIdAndStatDateBetweenOrderByStatDateDesc(
            coupleId, weekAgo, today);
        int totalQuizzes = recentStats.size();
        double avgScore = recentStats.stream().mapToInt(DailyStats::getMatchScore).average().orElse(0);

        List<AchievementResponse> recent = achievementRepo.findByCoupleId(coupleId).stream()
            .sorted((a, b) -> b.getUnlockedAt().compareTo(a.getUnlockedAt()))
            .limit(3)
            .map(a -> {
                AchievementType type = AchievementType.valueOf(a.getType());
                return new AchievementResponse(a.getType(), type.getDisplayName(), true, a.getUnlockedAt());
            })
            .toList();

        return new OverviewResponse(todayScore, streakDays, totalQuizzes, avgScore, recent);
    }

    public List<AchievementResponse> getAchievements(Long coupleId) {
        List<Achievement> unlocked = achievementRepo.findByCoupleId(coupleId);
        return Arrays.stream(AchievementType.values()).map(type -> {
            Achievement a = unlocked.stream()
                .filter(u -> u.getType().equals(type.name()))
                .findFirst().orElse(null);
            return new AchievementResponse(type.name(), type.getDisplayName(),
                a != null, a != null ? a.getUnlockedAt() : null);
        }).toList();
    }

    public WeeklyResponse getWeekly(Long coupleId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        List<DailyStats> stats = statsRepo.findByCoupleIdAndStatDateBetweenOrderByStatDateDesc(
            coupleId, start, end);

        List<DayScore> scores = stats.stream()
            .map(s -> new DayScore(s.getStatDate(), s.getMatchScore(),
                s.getQuizTypePlayed() != null ? s.getQuizTypePlayed().name() : null))
            .toList();

        double avg = stats.stream().mapToInt(DailyStats::getMatchScore).average().orElse(0);
        return new WeeklyResponse(scores, avg, stats.size());
    }
}
