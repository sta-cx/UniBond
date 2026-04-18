package com.unibond.quiz.service;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.quiz.entity.*;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuestionPoolRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizGenerationServiceTest {
    @Mock private DailyQuizRepository quizRepo;
    @Mock private QuestionPoolRepository poolRepo;
    @Mock private CoupleRepository coupleRepo;
    @Mock private UserRepository userRepo;
    @Mock private DailyStatsRepository statsRepo;
    @Mock private LlmService llmService;
    @Mock private com.unibond.push.service.PushService pushService;
    @InjectMocks private QuizGenerationService service;

    @Test
    void generateFallback_usesQuestionPool() {
        Couple couple = new Couple();
        couple.setId(1L);
        couple.setUserAId(10L);
        couple.setUserBId(11L);
        couple.setStatus(CoupleStatus.ACTIVE);
        when(coupleRepo.findByStatus(CoupleStatus.ACTIVE)).thenReturn(List.of(couple));
        when(quizRepo.findByCoupleIdAndDate(anyLong(), any())).thenReturn(java.util.Optional.empty());
        when(llmService.isEnabled()).thenReturn(false);

        QuestionPool q = new QuestionPool();
        q.setQuestion("你最喜欢的颜色？");
        q.setOptions("[\"红\",\"蓝\",\"绿\",\"黄\"]");
        when(poolRepo.findLeastUsedByType(any(QuizType.class), any(Pageable.class))).thenReturn(List.of(q, q, q, q, q));
        when(quizRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.generateDailyQuizzesFromPool();

        verify(quizRepo).save(argThat(quiz ->
            quiz.getGenerationSource() == GenerationSource.FALLBACK_POOL));
    }
}
