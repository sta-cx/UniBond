package com.unibond.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.push.service.PushService;
import com.unibond.quiz.entity.*;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuestionPoolRepository;
import com.unibond.stats.entity.DailyStats;
import com.unibond.stats.repository.DailyStatsRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
    private final DailyStatsRepository statsRepo;
    private final LlmService llmService;
    private final PushService pushService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizGenerationService(DailyQuizRepository quizRepo,
                                  QuestionPoolRepository poolRepo,
                                  CoupleRepository coupleRepo,
                                  UserRepository userRepo,
                                  DailyStatsRepository statsRepo,
                                  LlmService llmService,
                                  PushService pushService) {
        this.quizRepo = quizRepo;
        this.poolRepo = poolRepo;
        this.coupleRepo = coupleRepo;
        this.userRepo = userRepo;
        this.statsRepo = statsRepo;
        this.llmService = llmService;
        this.pushService = pushService;
    }

    @Scheduled(cron = "0 5 * * * *")
    public void scheduledGeneration() {
        Set<String> midnightZones = ZoneId.getAvailableZoneIds().stream()
            .filter(tz -> ZonedDateTime.now(ZoneId.of(tz)).getHour() == 0)
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

    public void generateDailyQuizzesFromPool() {
        LocalDate today = LocalDate.now();
        List<Couple> activeCouples = coupleRepo.findByStatus(CoupleStatus.ACTIVE);
        for (Couple couple : activeCouples) {
            generateForCouple(couple, today);
        }
    }

    private void generateForCouple(Couple couple, LocalDate today) {
        if (quizRepo.findByCoupleIdAndDate(couple.getId(), today).isPresent()) return;

        try {
            QuizType type = getQuizType(couple, today);
            String theme = getTheme(type, today);
            String questionsJson = null;
            GenerationSource source = GenerationSource.FALLBACK_POOL;

            if (llmService.isEnabled()) {
                String prompt = buildPrompt(couple, type, theme, today);
                String llmResult = llmService.generate(prompt);
                if (llmResult != null && validateQuestionsJson(llmResult)) {
                    questionsJson = llmResult;
                    source = GenerationSource.AI;
                    log.info("AI quiz generated for couple {}", couple.getId());
                }
            }

            if (questionsJson == null) {
                List<QuestionPool> questions = poolRepo.findLeastUsedByType(type, PageRequest.of(0, 5));
                questionsJson = objectMapper.writeValueAsString(
                    questions.stream().map(q -> java.util.Map.of(
                        "question", q.getQuestion(),
                        "options", q.getOptions()
                    )).collect(Collectors.toList())
                );
                questions.forEach(q -> {
                    q.setUsedCount(q.getUsedCount() + 1);
                    poolRepo.save(q);
                });
            }

            DailyQuiz quiz = new DailyQuiz();
            quiz.setDate(today);
            quiz.setCoupleId(couple.getId());
            quiz.setQuizType(type);
            quiz.setTheme(theme);
            quiz.setQuestions(questionsJson);
            quiz.setGenerationSource(source);
            quizRepo.save(quiz);

            notifyCouple(couple, "今日默契挑战来了，快来答题~", "QUIZ_GENERATED");

        } catch (Exception e) {
            log.error("Failed to generate quiz for couple {}", couple.getId(), e);
        }
    }

    private String buildPrompt(Couple couple, QuizType type, String theme, LocalDate today) {
        long daysBound = java.time.Duration.between(
            couple.getBindAt(), today.atStartOfDay(ZoneOffset.UTC).toInstant()
        ).toDays();

        var recentStats = statsRepo.findByCoupleIdAndStatDateBetweenOrderByStatDateDesc(
            couple.getId(), today.minusDays(7), today);
        double avgScore = recentStats.stream().mapToInt(DailyStats::getMatchScore).average().orElse(0);

        String typeDesc = switch (type) {
            case BLIND -> "BLIND（双方独立回答相同题目，比较默契度）";
            case GUESS -> "GUESS（关于伴侣习惯/喜好的题目，猜测对方的选择）";
            case THEME -> "THEME（围绕指定主题出题，主题：" + theme + "）";
        };

        return String.format("""
            你是一个为情侣设计趣味问答的专家。根据以下情侣信息，生成 %s 类型的 5 道选择题。

            情侣背景：
            - 绑定天数：%d天
            - 近期答题平均分：%.0f
            - 纪念日：今天%s纪念日

            要求：
            - 返回严格 JSON 格式：{"questions": [{"question": "题目", "options": ["选项A", "选项B", "选项C", "选项D"]}]}
            - 必须返回恰好 5 道题
            - 题目要有趣、贴近情侣日常
            - 每道题 4 个选项
            """, typeDesc, daysBound, avgScore,
            isAnniversary(couple, today) ? "是" : "不是");
    }

    private boolean validateQuestionsJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode questions = root.has("questions") ? root.get("questions") : root;
            if (!questions.isArray() || questions.size() != 5) return false;
            for (JsonNode q : questions) {
                if (!q.has("question") || !q.has("options")) return false;
                if (!q.get("options").isArray() || q.get("options").size() < 2) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static QuizType getQuizType(Couple couple, LocalDate date) {
        if (couple.getBindAt() != null) {
            LocalDate bindDate = couple.getBindAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (bindDate.getMonth() == date.getMonth() && bindDate.getDayOfMonth() == date.getDayOfMonth()) {
                return QuizType.THEME;
            }
        }
        return getQuizType(date);
    }

    static QuizType getQuizType(LocalDate date) {
        int day = (int)(date.toEpochDay() % 3);
        return switch (day) {
            case 0 -> QuizType.BLIND;
            case 1 -> QuizType.GUESS;
            default -> QuizType.THEME;
        };
    }

    private String getTheme(QuizType type, LocalDate date) {
        if (type != QuizType.THEME) return null;
        String[] themes = {"food", "travel", "memory", "daily", "interests"};
        return themes[(int)(date.toEpochDay() % themes.length)];
    }

    private boolean isAnniversary(Couple couple, LocalDate date) {
        if (couple.getBindAt() == null) return false;
        LocalDate bindDate = couple.getBindAt().atZone(ZoneOffset.UTC).toLocalDate();
        return bindDate.getMonth() == date.getMonth() && bindDate.getDayOfMonth() == date.getDayOfMonth();
    }

    private void notifyCouple(Couple couple, String message, String type) {
        for (Long uid : List.of(couple.getUserAId(), couple.getUserBId())) {
            userRepo.findById(uid).ifPresent(u -> {
                if (u.getDeviceToken() != null) {
                    pushService.sendNotification(u.getDeviceToken(), "UniBond", message, type, null);
                }
            });
        }
    }
}
