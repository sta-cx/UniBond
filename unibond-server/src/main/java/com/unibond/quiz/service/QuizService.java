package com.unibond.quiz.service;

import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.quiz.entity.QuizAnswer;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class QuizService {
    private final DailyQuizRepository quizRepo;
    private final QuizAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(DailyQuizRepository quizRepo, QuizAnswerRepository answerRepo,
                       UserRepository userRepo) {
        this.quizRepo = quizRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public QuizAnswer submitAnswer(Long userId, Long quizId, Long coupleId,
                                    String answers, String partnerGuess) {
        // Idempotent: return existing if already answered
        var existing = answerRepo.findByDailyQuizIdAndUserId(quizId, userId);
        if (existing.isPresent()) return existing.get();

        QuizAnswer answer = new QuizAnswer();
        answer.setDailyQuizId(quizId);
        answer.setUserId(userId);
        answer.setCoupleId(coupleId);
        answer.setAnswers(answers);
        answer.setPartnerGuess(partnerGuess);

        // Check if both answered → reveal
        long count = answerRepo.countByDailyQuizIdForUpdate(quizId);
        if (count >= 1) {
            // This is the second answer — calculate scores and reveal
            List<QuizAnswer> allAnswers = answerRepo.findByDailyQuizId(quizId);
            QuizAnswer first = allAnswers.get(0);

            int score = calculateScore(first.getAnswers(), answers);
            answer.setScore(score);
            first.setScore(score);
            answer.setRevealed(true);
            first.setRevealed(true);
            answerRepo.save(first);
        } else {
            answer.setRevealed(false);
        }

        return answerRepo.save(answer);
    }

    private int calculateScore(String answersA, String answersB) {
        try {
            List<String> a = objectMapper.readValue(answersA, new TypeReference<>() {});
            List<String> b = objectMapper.readValue(answersB, new TypeReference<>() {});
            int matches = 0;
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                if (a.get(i).equals(b.get(i))) matches++;
            }
            return Math.min(a.size(), b.size()) > 0 ? matches * 100 / Math.min(a.size(), b.size()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
