package com.unibond.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.quiz.entity.*;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuestionPoolRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizGenerationService {
    private static final Logger log = LoggerFactory.getLogger(QuizGenerationService.class);
    private final DailyQuizRepository quizRepo;
    private final QuestionPoolRepository poolRepo;
    private final CoupleRepository coupleRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizGenerationService(DailyQuizRepository quizRepo,
                                  QuestionPoolRepository poolRepo,
                                  CoupleRepository coupleRepo,
                                  UserRepository userRepo) {
        this.quizRepo = quizRepo;
        this.poolRepo = poolRepo;
        this.coupleRepo = coupleRepo;
        this.userRepo = userRepo;
    }

    // Run every hour to cover all timezones (generates only for couples whose local time is 00:xx)
    @Scheduled(cron = "0 5 * * * *")
    public void scheduledGeneration() {
        // Find timezones where it's currently midnight (00:00-00:59)
        Set<String> midnightZones = ZoneId.getAvailableZoneIds().stream()
            .filter(tz -> {
                int hour = ZonedDateTime.now(ZoneId.of(tz)).getHour();
                return hour == 0;
            })
            .collect(Collectors.toSet());

        List<Couple> activeCouples = coupleRepo.findByStatus(CoupleStatus.ACTIVE);

        for (Couple couple : activeCouples) {
            User userA = userRepo.findById(couple.getUserAId()).orElse(null);
            if (userA == null) continue;
            String tz = userA.getTimezone() != null ? userA.getTimezone() : "Asia/Shanghai";
            if (!midnightZones.contains(tz)) continue;

            LocalDate today = LocalDate.now(ZoneId.of(tz));
            generateForCouple(couple, today);
        }
    }

    // Fallback: generate from question pool (also used in tests)
    public void generateDailyQuizzesFromPool() {
        LocalDate today = LocalDate.now();
        List<Couple> activeCouples = coupleRepo.findByStatus(CoupleStatus.ACTIVE);
        for (Couple couple : activeCouples) {
            generateForCouple(couple, today);
        }
    }

    private void generateForCouple(Couple couple, LocalDate today) {
        if (quizRepo.findByCoupleIdAndDate(couple.getId(), today).isPresent()) {
            return; // already generated
        }
        try {
            QuizType type = getQuizType(today);
            List<QuestionPool> questions = poolRepo.findLeastUsedByType(type);
            String questionsJson = objectMapper.writeValueAsString(
                questions.stream().map(q -> java.util.Map.of(
                    "question", q.getQuestion(),
                    "options", q.getOptions()
                )).collect(Collectors.toList())
            );

            DailyQuiz quiz = new DailyQuiz();
            quiz.setDate(today);
            quiz.setCoupleId(couple.getId());
            quiz.setQuizType(type);
            quiz.setQuestions(questionsJson);
            quiz.setGenerationSource(GenerationSource.FALLBACK_POOL);
            quizRepo.save(quiz);

            questions.forEach(q -> {
                q.setUsedCount(q.getUsedCount() + 1);
                poolRepo.save(q);
            });
        } catch (Exception e) {
            log.error("Failed to generate quiz for couple {}", couple.getId(), e);
        }
    }

    static QuizType getQuizType(LocalDate date) {
        int day = (int)(date.toEpochDay() % 3);
        return switch (day) {
            case 0 -> QuizType.BLIND;
            case 1 -> QuizType.GUESS;
            default -> QuizType.THEME;
        };
    }
}
