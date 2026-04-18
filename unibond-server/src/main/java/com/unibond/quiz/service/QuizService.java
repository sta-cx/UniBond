package com.unibond.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.push.service.PushService;
import com.unibond.quiz.entity.DailyQuiz;
import com.unibond.quiz.entity.QuizAnswer;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.stats.entity.DailyStats;
import com.unibond.stats.repository.DailyStatsRepository;
import com.unibond.stats.service.AchievementService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {
    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private final DailyQuizRepository quizRepo;
    private final QuizAnswerRepository answerRepo;
    private final DailyStatsRepository statsRepo;
    private final CoupleRepository coupleRepo;
    private final UserRepository userRepo;
    private final AchievementService achievementService;
    private final PushService pushService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizService(DailyQuizRepository quizRepo, QuizAnswerRepository answerRepo,
                       DailyStatsRepository statsRepo, CoupleRepository coupleRepo,
                       UserRepository userRepo, AchievementService achievementService,
                       PushService pushService) {
        this.quizRepo = quizRepo;
        this.answerRepo = answerRepo;
        this.statsRepo = statsRepo;
        this.coupleRepo = coupleRepo;
        this.userRepo = userRepo;
        this.achievementService = achievementService;
        this.pushService = pushService;
    }

    @Transactional
    public QuizAnswer submitAnswer(Long userId, Long quizId, Long coupleId,
                                    String answers, String partnerGuess) {
        var existing = answerRepo.findByDailyQuizIdAndUserId(quizId, userId);
        if (existing.isPresent()) return existing.get();

        DailyQuiz quiz = quizRepo.findById(quizId)
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));
        if (!quiz.getCoupleId().equals(coupleId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if ("CANCELLED".equals(quiz.getStatus())) {
            throw new BizException(ErrorCode.QUIZ_CANCELLED);
        }

        QuizAnswer answer = new QuizAnswer();
        answer.setDailyQuizId(quizId);
        answer.setUserId(userId);
        answer.setCoupleId(coupleId);
        answer.setAnswers(answers);
        answer.setPartnerGuess(partnerGuess);

        long count = answerRepo.countByDailyQuizIdForUpdate(quizId);

        if (count >= 1) {
            List<QuizAnswer> allAnswers = answerRepo.findAllByDailyQuizIdForUpdate(quizId);
            QuizAnswer first = allAnswers.get(0);

            int score = calculateScore(quiz.getQuizType().name(), first, answer);
            answer.setScore(score);
            first.setScore(score);
            answer.setRevealed(true);
            first.setRevealed(true);
            answerRepo.save(first);

            saveDailyStats(coupleId, quiz, score);

            int streakDays = getStreakDays(coupleId, quiz.getDate());
            achievementService.checkAchievements(coupleId, score, streakDays,
                quiz.getQuizType().name(), quiz.getTheme());

            notifyCouple(coupleId, "默契结果出炉！今天你们得了" + score + "分", "QUIZ_REVEALED",
                Map.of("score", String.valueOf(score)));
        } else {
            answer.setRevealed(false);
            notifyPartner(coupleId, userId, "TA已经答完了，就等你啦！", "QUIZ_PARTNER_ANSWERED");
        }

        return answerRepo.save(answer);
    }

    private void saveDailyStats(Long coupleId, DailyQuiz quiz, int score) {
        int streakDays = getStreakDays(coupleId, quiz.getDate());
        DailyStats stats = new DailyStats();
        stats.setCoupleId(coupleId);
        stats.setStatDate(quiz.getDate());
        stats.setMatchScore(score);
        stats.setStreakDays(streakDays);
        stats.setQuizTypePlayed(quiz.getQuizType());
        statsRepo.save(stats);
    }

    private int getStreakDays(Long coupleId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        var yesterdayStats = statsRepo.findByCoupleIdAndStatDate(coupleId, yesterday);
        if (yesterdayStats.isPresent() && yesterdayStats.get().getStreakDays() > 0) {
            return yesterdayStats.get().getStreakDays() + 1;
        }
        return 1;
    }

    private int calculateScore(String quizType, QuizAnswer first, QuizAnswer second) {
        if ("GUESS".equals(quizType)) {
            return calculateGuessScore(first, second);
        }
        return calculateBlindScore(first.getAnswers(), second.getAnswers());
    }

    private int calculateBlindScore(String answersA, String answersB) {
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

    private int calculateGuessScore(QuizAnswer a, QuizAnswer b) {
        try {
            List<String> aGuess = parseJson(a.getPartnerGuess());
            List<String> bAnswers = parseJson(b.getAnswers());
            List<String> bGuess = parseJson(b.getPartnerGuess());
            List<String> aAnswers = parseJson(a.getAnswers());

            int aCorrect = countMatches(aGuess, bAnswers);
            int bCorrect = countMatches(bGuess, aAnswers);
            int total = Math.min(aGuess.size(), bAnswers.size()) + Math.min(bGuess.size(), aAnswers.size());
            return total > 0 ? (aCorrect + bCorrect) * 100 / total : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> parseJson(String json) throws Exception {
        if (json == null || json.isBlank()) return List.of();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private int countMatches(List<String> guess, List<String> actual) {
        int matches = 0;
        for (int i = 0; i < Math.min(guess.size(), actual.size()); i++) {
            if (guess.get(i).equals(actual.get(i))) matches++;
        }
        return matches;
    }

    private void notifyCouple(Long coupleId, String message, String type, Map<String, String> data) {
        Couple couple = coupleRepo.findById(coupleId).orElse(null);
        if (couple == null) return;
        for (Long uid : List.of(couple.getUserAId(), couple.getUserBId())) {
            userRepo.findById(uid).ifPresent(u -> {
                if (u.getDeviceToken() != null) {
                    pushService.sendNotification(u.getDeviceToken(), "UniBond", message, type, data);
                }
            });
        }
    }

    private void notifyPartner(Long coupleId, Long userId, String message, String type) {
        Couple couple = coupleRepo.findById(coupleId).orElse(null);
        if (couple == null) return;
        Long partnerId = couple.getUserAId().equals(userId) ? couple.getUserBId() : couple.getUserAId();
        userRepo.findById(partnerId).ifPresent(u -> {
            if (u.getDeviceToken() != null) {
                pushService.sendNotification(u.getDeviceToken(), "UniBond", message, type, null);
            }
        });
    }
}
