package com.unibond.stats.service;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.push.service.PushService;
import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {
    @Mock private AchievementRepository achievementRepo;
    @Mock private DailyStatsRepository statsRepo;
    @Mock private CoupleRepository coupleRepo;
    @Mock private UserRepository userRepo;
    @Mock private PushService pushService;
    @InjectMocks private AchievementService service;

    @Test
    void checkAchievements_streak7_unlocks() {
        when(achievementRepo.existsByCoupleIdAndType(eq(1L), anyString()))
            .thenReturn(false);
        when(statsRepo.countByCoupleIdAndMatchScoreGreaterThanEqual(eq(1L), eq(90))).thenReturn(0L);
        when(achievementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(coupleRepo.findById(1L)).thenReturn(java.util.Optional.empty());

        service.checkAchievements(1L, 50, 7, "BLIND", null);

        verify(achievementRepo).save(argThat(a ->
            a.getType().equals(AchievementType.STREAK_7.name())));
    }

    @Test
    void checkAchievements_perfectScore_unlocks() {
        when(achievementRepo.existsByCoupleIdAndType(eq(1L), anyString()))
            .thenReturn(false);
        when(achievementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkAchievements(1L, 100, 1, "BLIND", null);

        verify(achievementRepo).save(argThat(a ->
            a.getType().equals(AchievementType.PERFECT_MATCH.name())));
    }
}
