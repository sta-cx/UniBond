package com.unibond.stats.service;

import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {
    @Mock private AchievementRepository achievementRepo;
    @Mock private DailyStatsRepository statsRepo;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @InjectMocks private AchievementService service;

    @Test
    void checkAchievements_streak7_unlocks() {
        when(achievementRepo.existsByCoupleIdAndType(1L, AchievementType.STREAK_7.name()))
            .thenReturn(false);
        when(achievementRepo.existsByCoupleIdAndType(1L, AchievementType.STREAK_3.name()))
            .thenReturn(true);
        when(achievementRepo.existsByCoupleIdAndType(1L, AchievementType.PERFECT_MATCH.name()))
            .thenReturn(true);
        when(achievementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkAchievements(1L, 100, 7);

        verify(achievementRepo).save(argThat(a ->
            a.getType().equals(AchievementType.STREAK_7.name())));
    }

    @Test
    void checkAchievements_perfectScore_unlocks() {
        when(achievementRepo.existsByCoupleIdAndType(1L, AchievementType.PERFECT_MATCH.name()))
            .thenReturn(false);
        when(achievementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkAchievements(1L, 100, 1);

        verify(achievementRepo).save(argThat(a ->
            a.getType().equals(AchievementType.PERFECT_MATCH.name())));
    }
}
