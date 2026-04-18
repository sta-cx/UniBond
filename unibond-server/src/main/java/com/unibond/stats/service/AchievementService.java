package com.unibond.stats.service;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.push.service.PushService;
import com.unibond.stats.entity.Achievement;
import com.unibond.stats.entity.AchievementType;
import com.unibond.stats.repository.AchievementRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AchievementService {
    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);
    private final AchievementRepository achievementRepo;
    private final DailyStatsRepository statsRepo;
    private final CoupleRepository coupleRepo;
    private final UserRepository userRepo;
    private final PushService pushService;

    public AchievementService(AchievementRepository achievementRepo,
                               DailyStatsRepository statsRepo,
                               CoupleRepository coupleRepo,
                               UserRepository userRepo,
                               PushService pushService) {
        this.achievementRepo = achievementRepo;
        this.statsRepo = statsRepo;
        this.coupleRepo = coupleRepo;
        this.userRepo = userRepo;
        this.pushService = pushService;
    }

    @Transactional
    public void checkAchievements(Long coupleId, int score, int streakDays,
                                   String quizType, String theme) {
        tryUnlock(coupleId, AchievementType.STREAK_3, () -> streakDays >= 3);
        tryUnlock(coupleId, AchievementType.STREAK_7, () -> streakDays >= 7);
        tryUnlock(coupleId, AchievementType.STREAK_30, () -> streakDays >= 30);
        tryUnlock(coupleId, AchievementType.STREAK_100, () -> streakDays >= 100);
        tryUnlock(coupleId, AchievementType.PERFECT_MATCH, () -> score == 100);
        tryUnlock(coupleId, AchievementType.HIGH_SCORE_10,
            () -> statsRepo.countByCoupleIdAndMatchScoreGreaterThanEqual(coupleId, 90) >= 10);
        tryUnlock(coupleId, AchievementType.THEME_FOOD,
            () -> "THEME".equals(quizType) && "food".equals(theme));
        tryUnlock(coupleId, AchievementType.THEME_TRAVEL,
            () -> "THEME".equals(quizType) && "travel".equals(theme));
        tryUnlock(coupleId, AchievementType.THEME_MEMORY,
            () -> "THEME".equals(quizType) && "memory".equals(theme));
        tryUnlock(coupleId, AchievementType.ANNIVERSARY, () -> isAnniversary(coupleId));
    }

    @Transactional
    public void grantFirstBind(Long coupleId) {
        tryUnlock(coupleId, AchievementType.FIRST_BIND, () -> true);
    }

    @FunctionalInterface
    interface Condition { boolean met(); }

    private void tryUnlock(Long coupleId, AchievementType type, Condition condition) {
        if (!condition.met()) return;
        if (achievementRepo.existsByCoupleIdAndType(coupleId, type.name())) return;

        Achievement a = new Achievement();
        a.setCoupleId(coupleId);
        a.setType(type.name());
        achievementRepo.save(a);

        notifyCouple(coupleId, "解锁新成就：" + type.getDisplayName());
        log.info("Achievement unlocked: {} for couple {}", type.name(), coupleId);
    }

    private boolean isAnniversary(Long coupleId) {
        Couple couple = coupleRepo.findById(coupleId).orElse(null);
        if (couple == null || couple.getBindAt() == null) return false;
        LocalDate bindDate = couple.getBindAt().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate today = LocalDate.now();
        return bindDate.getMonth() == today.getMonth() && bindDate.getDayOfMonth() == today.getDayOfMonth();
    }

    private void notifyCouple(Long coupleId, String message) {
        Couple couple = coupleRepo.findById(coupleId).orElse(null);
        if (couple == null) return;
        for (Long uid : List.of(couple.getUserAId(), couple.getUserBId())) {
            userRepo.findById(uid).ifPresent(u -> {
                if (u.getDeviceToken() != null) {
                    pushService.sendNotification(u.getDeviceToken(),
                        "UniBond", message, "ACHIEVEMENT_UNLOCKED", null);
                }
            });
        }
    }
}
