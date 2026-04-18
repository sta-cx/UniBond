package com.unibond.couple.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoupleService {
    private final UserRepository userRepository;
    private final CoupleRepository coupleRepository;
    private final StringRedisTemplate redisTemplate;

    public CoupleService(UserRepository userRepository, CoupleRepository coupleRepository,
                         StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.coupleRepository = coupleRepository;
        this.redisTemplate = redisTemplate;
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

        // 1. Dissolve couple
        coupleRepository.findActiveByUserId(userId)
            .ifPresent(c -> {
                c.setStatus(CoupleStatus.DISSOLVED);
                coupleRepository.save(c);
            });

        // 2. Clear partner IDs
        me.setPartnerId(null);
        partner.setPartnerId(null);
        userRepository.save(me);
        userRepository.save(partner);

        // 3. Revoke refresh tokens for both users
        redisTemplate.delete("refresh:" + me.getId());
        redisTemplate.delete("refresh:" + partner.getId());

        // 4. Cancel today's DailyQuiz + send APNs end event + push notification
        //    are handled by the caller (controller) via PushService
    }
}
