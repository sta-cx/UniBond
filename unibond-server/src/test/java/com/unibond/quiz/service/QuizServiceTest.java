package com.unibond.quiz.service;

import com.unibond.couple.repository.CoupleRepository;
import com.unibond.quiz.entity.*;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.stats.repository.DailyStatsRepository;
import com.unibond.stats.service.AchievementService;
import com.unibond.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock private DailyQuizRepository quizRepo;
    @Mock private QuizAnswerRepository answerRepo;
    @Mock private DailyStatsRepository statsRepo;
    @Mock private CoupleRepository coupleRepo;
    @Mock private UserRepository userRepo;
    @Mock private AchievementService achievementService;
    @Mock private com.unibond.push.service.PushService pushService;

    private QuizService quizService;

    @BeforeEach
    void setUp() {
        quizService = new QuizService(quizRepo, answerRepo, statsRepo, coupleRepo,
            userRepo, achievementService, pushService);
    }

    @Test
    void submitAnswer_idempotent_returnsExisting() {
        QuizAnswer existing = new QuizAnswer();
        existing.setId(1L);
        when(answerRepo.findByDailyQuizIdAndUserId(10L, 1L))
            .thenReturn(Optional.of(existing));

        QuizAnswer result = quizService.submitAnswer(1L, 10L, 5L, "[1,2,3,4,5]", null);
        assertEquals(1L, result.getId());
        verify(answerRepo, never()).save(any());
    }

    @Test
    void submitAnswer_secondSubmission_triggersReveal() {
        when(answerRepo.findByDailyQuizIdAndUserId(10L, 2L))
            .thenReturn(Optional.empty());

        DailyQuiz quiz = new DailyQuiz();
        quiz.setId(10L);
        quiz.setCoupleId(5L);
        quiz.setQuizType(QuizType.BLIND);
        quiz.setStatus("ACTIVE");
        quiz.setDate(java.time.LocalDate.now());
        when(quizRepo.findById(10L)).thenReturn(Optional.of(quiz));

        QuizAnswer firstAnswer = new QuizAnswer();
        firstAnswer.setId(1L);
        firstAnswer.setUserId(1L);
        firstAnswer.setAnswers("[\"A\",\"B\",\"C\",\"D\",\"A\"]");
        firstAnswer.setRevealed(false);

        when(answerRepo.countByDailyQuizIdForUpdate(10L)).thenReturn(1L);
        when(answerRepo.findAllByDailyQuizIdForUpdate(10L)).thenReturn(List.of(firstAnswer));
        when(statsRepo.findByCoupleIdAndStatDate(eq(5L), any())).thenReturn(Optional.empty());
        when(answerRepo.save(any())).thenAnswer(inv -> {
            QuizAnswer a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        QuizAnswer result = quizService.submitAnswer(2L, 10L, 5L,
            "[\"A\",\"B\",\"C\",\"D\",\"A\"]", null);

        assertTrue(result.getRevealed());
        assertTrue(firstAnswer.getRevealed());
    }
}
