package com.unibond.mood.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.mood.entity.MoodStatus;
import com.unibond.mood.repository.MoodRepository;
import com.unibond.push.service.LiveActivityPushService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class MoodService {
    private final MoodRepository moodRepo;
    private final UserRepository userRepo;
    private final CoupleService coupleService;
    private final LiveActivityPushService liveActivityPush;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MoodService(MoodRepository moodRepo, UserRepository userRepo,
                       CoupleService coupleService, LiveActivityPushService liveActivityPush,
                       StringRedisTemplate redisTemplate) {
        this.moodRepo = moodRepo;
        this.userRepo = userRepo;
        this.coupleService = coupleService;
        this.liveActivityPush = liveActivityPush;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public MoodStatus updateMood(Long userId, String emoji, String text) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        Couple couple = coupleService.getActiveCouple(userId);

        MoodStatus mood = new MoodStatus();
        mood.setUserId(userId);
        mood.setCoupleId(couple.getId());
        mood.setMoodEmoji(emoji);
        mood.setMoodText(text);
        mood.setUpdatedAt(java.time.Instant.now());
        mood = moodRepo.save(mood);

        // Cache in Redis
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                "emoji", emoji,
                "text", text != null ? text : "",
                "updatedAt", mood.getUpdatedAt().toString()
            ));
            redisTemplate.opsForValue().set("mood:" + userId, json);
        } catch (Exception ignored) {}

        // Trigger Live Activity update to partner via APNs
        Long partnerId = couple.getUserAId().equals(userId)
            ? couple.getUserBId() : couple.getUserAId();
        User partner = userRepo.findById(partnerId).orElse(null);
        if (partner != null && partner.getDeviceToken() != null) {
            liveActivityPush.updateMoodActivity(partner.getDeviceToken(), emoji, text);
        }

        return mood;
    }

    public MoodStatus getPartnerMood(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (user.getPartnerId() == null) throw new BizException(ErrorCode.COUPLE_NOT_BOUND);

        return moodRepo.findTopByUserIdOrderByUpdatedAtDesc(user.getPartnerId())
            .orElse(null);
    }
}
