package com.unibond.quiz.service;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.repository.CoupleRepository;
import com.unibond.push.service.PushService;
import com.unibond.quiz.entity.DailyQuiz;
import com.unibond.quiz.entity.QuizAnswer;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class QuizReminderService {
    private static final Logger log = LoggerFactory.getLogger(QuizReminderService.class);
    private final DailyQuizRepository quizRepo;
    private final QuizAnswerRepository answerRepo;
    private final CoupleRepository coupleRepo;
    private final UserRepository userRepo;
    private final PushService pushService;

    public QuizReminderService(DailyQuizRepository quizRepo,
                                QuizAnswerRepository answerRepo,
                                CoupleRepository coupleRepo,
                                UserRepository userRepo,
                                PushService pushService) {
        this.quizRepo = quizRepo;
        this.answerRepo = answerRepo;
        this.coupleRepo = coupleRepo;
        this.userRepo = userRepo;
        this.pushService = pushService;
    }

    @Scheduled(cron = "0 0 20 * * *")
    public void remindUnansweredQuizzes() {
        LocalDate today = LocalDate.now();
        List<DailyQuiz> activeQuizzes = quizRepo.findByDateAndStatus(today, "ACTIVE");
        int reminded = 0;

        for (DailyQuiz quiz : activeQuizzes) {
            Couple couple = coupleRepo.findById(quiz.getCoupleId()).orElse(null);
            if (couple == null) continue;

            List<QuizAnswer> answers = answerRepo.findByDailyQuizId(quiz.getId());
            List<Long> answeredIds = answers.stream().map(QuizAnswer::getUserId).toList();

            for (Long uid : List.of(couple.getUserAId(), couple.getUserBId())) {
                if (answeredIds.contains(uid)) continue;
                User user = userRepo.findById(uid).orElse(null);
                if (user != null && user.getDeviceToken() != null) {
                    pushService.sendNotification(user.getDeviceToken(),
                        "UniBond", "想TA了吗？今天的默契题还没答哦", "QUIZ_REMINDER", null);
                    reminded++;
                }
            }
        }
        if (reminded > 0) log.info("Sent {} quiz reminder pushes", reminded);
    }
}
