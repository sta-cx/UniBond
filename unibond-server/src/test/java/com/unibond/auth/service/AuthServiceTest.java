package com.unibond.auth.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.security.JwtProvider;
import com.unibond.user.entity.AuthProvider;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, jwtProvider, redisTemplate,
            "ABCDEFGHJKMNPQRSTUVWXYZ23456789", 6, 3);
    }

    @Test
    void emailLogin_newUser_createsUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("email_code:test@test.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        when(userRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtProvider.createAccessToken(1L)).thenReturn("access");
        when(jwtProvider.createRefreshToken(1L)).thenReturn("refresh");
        when(redisTemplate.delete("email_code:test@test.com")).thenReturn(true);

        var response = authService.emailLogin("test@test.com", "123456");

        assertEquals("access", response.accessToken());
        assertEquals("refresh", response.refreshToken());
        verify(userRepository).save(any());
    }

    @Test
    void emailLogin_wrongCode_throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("email_code:test@test.com")).thenReturn("999999");

        BizException ex = assertThrows(BizException.class,
            () -> authService.emailLogin("test@test.com", "123456"));
        assertEquals(ErrorCode.AUTH_CODE_INVALID, ex.getErrorCode());
    }

    @Test
    void emailLogin_expiredCode_throws() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("email_code:test@test.com")).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
            () -> authService.emailLogin("test@test.com", "123456"));
        assertEquals(ErrorCode.AUTH_CODE_EXPIRED, ex.getErrorCode());
    }
}
