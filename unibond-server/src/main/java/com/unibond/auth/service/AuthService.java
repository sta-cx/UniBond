package com.unibond.auth.service;

import com.unibond.auth.dto.AuthResponse;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.security.JwtProvider;
import com.unibond.user.entity.AuthProvider;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final String inviteCharset;
    private final int inviteLength;
    private final int inviteMaxRetries;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository, JwtProvider jwtProvider,
                       StringRedisTemplate redisTemplate,
                       @Value("${app.invite-code.charset}") String inviteCharset,
                       @Value("${app.invite-code.length}") int inviteLength,
                       @Value("${app.invite-code.max-retries}") int inviteMaxRetries) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.redisTemplate = redisTemplate;
        this.inviteCharset = inviteCharset;
        this.inviteLength = inviteLength;
        this.inviteMaxRetries = inviteMaxRetries;
    }

    @Transactional
    public AuthResponse emailLogin(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get("email_code:" + email);
        if (storedCode == null) throw new BizException(ErrorCode.AUTH_CODE_EXPIRED);
        if (!storedCode.equals(code)) throw new BizException(ErrorCode.AUTH_CODE_INVALID);

        boolean isNew = false;
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setAuthProvider(AuthProvider.EMAIL);
            user.setInviteCode(generateInviteCode());
            user = userRepository.save(user);
            isNew = true;
        }

        redisTemplate.delete("email_code:" + email);
        return issueTokens(user, isNew);
    }

    @Transactional
    public AuthResponse appleLogin(String appleSub, String nickname, String timezone) {
        boolean isNew = false;
        User user = userRepository.findByAppleSub(appleSub).orElse(null);
        if (user == null) {
            user = new User();
            user.setAppleSub(appleSub);
            user.setAuthProvider(AuthProvider.APPLE);
            user.setNickname(nickname);
            user.setTimezone(timezone);
            user.setInviteCode(generateInviteCode());
            user = userRepository.save(user);
            isNew = true;
        }
        return issueTokens(user, isNew);
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtProvider.validate(refreshToken)) {
            throw new BizException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        if (!"refresh".equals(jwtProvider.getType(refreshToken))) {
            throw new BizException(ErrorCode.INVALID_TOKEN_TYPE);
        }
        Long userId = jwtProvider.getUserId(refreshToken);
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new BizException(ErrorCode.AUTH_TOKEN_INVALID);
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        return issueTokens(user, false);
    }

    public void logout(Long userId) {
        redisTemplate.delete("refresh:" + userId);
    }

    public void logoutByRefreshToken(String refreshToken) {
        if (refreshToken != null && jwtProvider.validate(refreshToken)) {
            Long userId = jwtProvider.getUserId(refreshToken);
            redisTemplate.delete("refresh:" + userId);
        }
    }

    private AuthResponse issueTokens(User user, boolean isNew) {
        String access = jwtProvider.createAccessToken(user.getId());
        String refresh = jwtProvider.createRefreshToken(user.getId());
        // Token rotation: overwrite old refresh token in Redis
        redisTemplate.opsForValue().set("refresh:" + user.getId(), refresh, 30, TimeUnit.DAYS);
        return new AuthResponse(access, refresh, user.getId(), isNew);
    }

    private String generateInviteCode() {
        for (int i = 0; i < inviteMaxRetries; i++) {
            StringBuilder sb = new StringBuilder(inviteLength);
            for (int j = 0; j < inviteLength; j++) {
                sb.append(inviteCharset.charAt(random.nextInt(inviteCharset.length())));
            }
            String code = sb.toString();
            if (!userRepository.existsByInviteCode(code)) return code;
        }
        throw new RuntimeException("Failed to generate unique invite code");
    }

    public void sendEmailCode(String email) {
        // Rate limit: 1 per 60s per email
        String rateLimitKey = "email_rate:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new BizException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        // Hourly limit: 5 per hour per email
        String hourlyKey = "email_hourly:" + email;
        String hourlyCount = redisTemplate.opsForValue().get(hourlyKey);
        if (hourlyCount != null && Integer.parseInt(hourlyCount) >= 5) {
            throw new BizException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        String code = String.format("%06d", random.nextInt(1000000));
        redisTemplate.opsForValue().set("email_code:" + email, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(rateLimitKey, "1", 60, TimeUnit.SECONDS);
        redisTemplate.opsForValue().increment(hourlyKey);
        redisTemplate.expire(hourlyKey, 1, TimeUnit.HOURS);
    }

    public String getEmailCode(String email) {
        return redisTemplate.opsForValue().get("email_code:" + email);
    }

    public void cleanupEmailCode(String email) {
        redisTemplate.delete("email_code:" + email);
        redisTemplate.delete("email_rate:" + email);
        redisTemplate.delete("email_hourly:" + email);
    }
}
