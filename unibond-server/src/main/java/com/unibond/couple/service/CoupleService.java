package com.unibond.couple.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.push.service.LiveActivityPushService;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.stats.service.AchievementService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
public class CoupleService {
    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;
    private final DailyQuizRepository quizRepo;
    private final StringRedisTemplate redisTemplate;
    private final LiveActivityPushService liveActivityPush;
    private final AchievementService achievementService;

    public CoupleService(UserRepository userRepository, CoupleRepository coupleRepository,
                         DailyQuizRepository quizRepo, StringRedisTemplate redisTemplate,
                         LiveActivityPushService liveActivityPush,
                         AchievementService achievementService) {
        this.userRepository = userRepository;
        this.coupleRepository = coupleRepository;
        this.quizRepo = quizRepo;
        this.redisTemplate = redisTemplate;
        this.liveActivityPush = liveActivityPush;
        this.achievementService = achievementService;
    }

    public Couple getActiveCouple(Long userId) {
        return coupleRepository.findActiveByUserId(userId)
            .orElseThrow(() -> new BizException(ErrorCode.COUPLE_NOT_BOUND));
    }

    @Transactional
    public Couple bind(Long userId, String inviteCode) {
        User me = userRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        if (me.getPartnerId() != null) {
            throw new BizException(ErrorCode.COUPLE_ALREADY_BOUND);
        }

        User partner = userRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new BizException(ErrorCode.INVITE_CODE_INVALID));

        partner = userRepository.findByIdForUpdate(partner.getId())
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        if (partner.getId().equals(userId)) {
            throw new BizException(ErrorCode.INVITE_CODE_SELF);
        }
        if (partner.getPartnerId() != null) {
            throw new BizException(ErrorCode.COUPLE_ALREADY_BOUND);
        }

        Couple couple = new Couple();
        couple.setUserAId(userId);
        couple.setUserBId(partner.getId());
        couple.setStatus(CoupleStatus.ACTIVE);
        couple.setBindAt(java.time.Instant.now());
        couple = coupleRepository.save(couple);

        me.setPartnerId(partner.getId());
        partner.setPartnerId(me.getId());
        userRepository.save(me);
        userRepository.save(partner);

        achievementService.grantFirstBind(couple.getId());
        return couple;
    }

    @Transactional
    public void unbind(Long userId) {
        User me = userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (me.getPartnerId() == null) {
            throw new BizException(ErrorCode.COUPLE_NOT_BOUND);
        }

        User partner = userRepository.findById(me.getPartnerId())
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        Couple couple = coupleRepository.findActiveByUserId(userId).orElse(null);

        // 1. Cancel today's active quiz
        if (couple != null) {
            LocalDate today = LocalDate.now();
            quizRepo.findByCoupleIdAndDate(couple.getId(), today).ifPresent(quiz -> {
                if ("ACTIVE".equals(quiz.getStatus())) {
                    quiz.setStatus("CANCELLED");
                    quizRepo.save(quiz);
                }
            });
        }

        // 2. Dissolve couple
        if (couple != null) {
            couple.setStatus(CoupleStatus.DISSOLVED);
            coupleRepository.save(couple);
        }

        // 3. Clear partner IDs
        me.setPartnerId(null);
        partner.setPartnerId(null);
        userRepository.save(me);
        userRepository.save(partner);

        // 4. End partner's Live Activity
        if (partner.getDeviceToken() != null) {
            liveActivityPush.endActivity(partner.getDeviceToken());
        }

        // 5. Revoke refresh tokens
        redisTemplate.delete("refresh:" + me.getId());
        redisTemplate.delete("refresh:" + partner.getId());
    }

    @Transactional
    public Couple updateAnniversary(Long userId, LocalDate anniversaryDate) {
        Couple couple = getActiveCouple(userId);
        couple.setAnniversaryDate(anniversaryDate);
        return coupleRepository.save(couple);
    }
}
