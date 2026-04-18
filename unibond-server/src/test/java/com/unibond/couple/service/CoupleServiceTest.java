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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoupleServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private CoupleRepository coupleRepository;
    @Mock private DailyQuizRepository quizRepo;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private LiveActivityPushService liveActivityPush;
    @Mock private AchievementService achievementService;
    @InjectMocks private CoupleService coupleService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setInviteCode("ABC123");

        userB = new User();
        userB.setId(2L);
        userB.setInviteCode("XYZ789");
    }

    @Test
    void bind_success() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(userA));
        when(userRepository.findByInviteCode("XYZ789")).thenReturn(Optional.of(userB));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(userB));
        when(coupleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Couple couple = coupleService.bind(1L, "XYZ789");

        assertEquals(1L, couple.getUserAId());
        assertEquals(2L, couple.getUserBId());
        assertEquals(CoupleStatus.ACTIVE, couple.getStatus());
        assertEquals(2L, userA.getPartnerId());
        assertEquals(1L, userB.getPartnerId());
    }

    @Test
    void bind_selfInviteCode_throws() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(userA));
        when(userRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(userA));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(userA));

        BizException ex = assertThrows(BizException.class,
            () -> coupleService.bind(1L, "ABC123"));
        assertEquals(ErrorCode.INVITE_CODE_SELF, ex.getErrorCode());
    }

    @Test
    void bind_alreadyBound_throws() {
        userA.setPartnerId(3L);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(userA));

        BizException ex = assertThrows(BizException.class,
            () -> coupleService.bind(1L, "XYZ789"));
        assertEquals(ErrorCode.COUPLE_ALREADY_BOUND, ex.getErrorCode());
    }
}
