package com.unibond.mood.service;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.service.CoupleService;
import com.unibond.mood.entity.MoodStatus;
import com.unibond.mood.repository.MoodRepository;
import com.unibond.push.service.LiveActivityPushService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoodServiceTest {
    @Mock private MoodRepository moodRepo;
    @Mock private UserRepository userRepo;
    @Mock private CoupleService coupleService;
    @Mock private LiveActivityPushService liveActivityPush;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @InjectMocks private MoodService moodService;

    @Test
    void updateMood_savesToDbAndRedisAndPushes() {
        User user = new User();
        user.setId(1L);
        user.setPartnerId(2L);
        User partner = new User();
        partner.setId(2L);
        partner.setDeviceToken("token123");

        Couple couple = new Couple();
        couple.setId(10L);
        couple.setUserAId(1L);
        couple.setUserBId(2L);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(userRepo.findById(2L)).thenReturn(Optional.of(partner));
        when(coupleService.getActiveCouple(1L)).thenReturn(couple);
        when(moodRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        MoodStatus result = moodService.updateMood(1L, "😊", "今天真开心");

        assertEquals("😊", result.getMoodEmoji());
        assertEquals("今天真开心", result.getMoodText());
        verify(moodRepo).save(any());
        verify(valueOps).set(eq("mood:1"), anyString());
        verify(liveActivityPush).updateMoodActivity("token123", "😊", "今天真开心");
    }
}
