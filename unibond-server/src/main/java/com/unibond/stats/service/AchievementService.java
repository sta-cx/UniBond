package com.unibond.stats.service;

import com.unibond.stats.entity.Achievement;
import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class AchievementService {
    private final AchievementRepository achievementRepo;
    private final DailyStatsRepository statsRepo;
    private final StringRedisTemplate redisTemplate;

    public AchievementService(AchievementRepository achievementRepo,
                               DailyStatsRepository statsRepo,
                               StringRedisTemplate redisTemplate) {
        this.achievementRepo = achievementRepo;
        this.statsRepo = statsRepo;
        this.redisTemplate = redisTemplate;
    }

    public List<Achievement> checkAchievements(Long coupleId, int score, int streakDays) {
        List<Achievement> unlocked = new ArrayList<>();

        // Streak achievements
        if (streakDays >= 3) tryUnlock(coupleId, AchievementType.STREAK_3, unlocked);
        if (streakDays >= 7) tryUnlock(coupleId, AchievementType.STREAK_7, unlocked);
        if (streakDays >= 30) tryUnlock(coupleId, AchievementType.STREAK_30, unlocked);
        if (streakDays >= 100) tryUnlock(coupleId, AchievementType.STREAK_100, unlocked);

        // Score achievements
        if (score == 100) tryUnlock(coupleId, AchievementType.PERFECT_MATCH, unlocked);

        return unlocked;
    }

    private void tryUnlock(Long coupleId, AchievementType type, List<Achievement> unlocked) {
        if (!achievementRepo.existsByCoupleIdAndType(coupleId, type.name())) {
            Achievement a = new Achievement();
            a.setCoupleId(coupleId);
            a.setType(type.name());
            unlocked.add(achievementRepo.save(a));
        }
    }
}
