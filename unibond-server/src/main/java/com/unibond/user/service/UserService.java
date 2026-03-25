package com.unibond.user.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.util.InputSanitizer;
import com.unibond.couple.service.CoupleService;
import com.unibond.user.dto.UserResponse;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CoupleService coupleService;
    private final StringRedisTemplate redisTemplate;

    public UserService(UserRepository userRepository, CoupleService coupleService,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.coupleService = coupleService;
        this.redisTemplate = redisTemplate;
    }

    public UserResponse getMe(Long userId) {
        User u = findUser(userId);
        return toResponse(u);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, String nickname, String avatarUrl) {
        User u = findUser(userId);
        if (nickname != null) u.setNickname(InputSanitizer.sanitizeText(nickname, 20));
        if (avatarUrl != null) u.setAvatarUrl(avatarUrl);
        return toResponse(userRepository.save(u));
    }

    @Transactional
    public void updateDeviceToken(Long userId, String deviceToken) {
        User u = findUser(userId);
        u.setDeviceToken(deviceToken);
        userRepository.save(u);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = findUser(userId);

        // 1. Unbind couple if bound (handles partner cleanup)
        if (user.getPartnerId() != null) {
            coupleService.unbind(userId);
        }

        // 2. Revoke refresh token
        redisTemplate.delete("refresh:" + userId);

        // 3. Nullify FK references in quiz_answers, mood_status (set user_id references)
        //    Note: historical data preserved per spec, but user record removed
        //    DB schema should use ON DELETE SET NULL for user FK references
        userRepository.deleteById(userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getNickname(), u.getAvatarUrl(),
            u.getAuthProvider(), u.getInviteCode(), u.getPartnerId(), u.getCreatedAt());
    }
}
