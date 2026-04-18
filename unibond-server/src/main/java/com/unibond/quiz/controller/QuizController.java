package com.unibond.quiz.controller;

import com.unibond.common.dto.ApiResponse;
import com.unibond.common.dto.CursorPage;
import com.unibond.common.exception.BizException;
import com.unibond.common.exception.ErrorCode;
import com.unibond.common.security.UserPrincipal;
import com.unibond.couple.entity.Couple;
import com.unibond.couple.service.CoupleService;
import com.unibond.quiz.dto.*;
import com.unibond.quiz.entity.DailyQuiz;
import com.unibond.quiz.entity.QuizAnswer;
import com.unibond.quiz.repository.DailyQuizRepository;
import com.unibond.quiz.repository.QuizAnswerRepository;
import com.unibond.quiz.service.QuizService;
import com.unibond.user.entity.User;
import com.unibond.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quiz")
public class QuizController {
    private final QuizService quizService;
    private final DailyQuizRepository quizRepo;
    private final QuizAnswerRepository answerRepo;
    private final UserRepository userRepo;
    private final CoupleService coupleService;

    public QuizController(QuizService quizService, DailyQuizRepository quizRepo,
                          QuizAnswerRepository answerRepo, UserRepository userRepo,
                          CoupleService coupleService) {
        this.quizService = quizService;
        this.quizRepo = quizRepo;
        this.answerRepo = answerRepo;
        this.userRepo = userRepo;
        this.coupleService = coupleService;
    }

    @GetMapping("/today")
    public ApiResponse<QuizResponse> today(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepo.findById(principal.userId()).orElseThrow();
        Couple couple = coupleService.getActiveCouple(principal.userId());
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

        DailyQuiz quiz = quizRepo.findByCoupleIdAndDate(couple.getId(), today)
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));

        return ApiResponse.ok(new QuizResponse(quiz.getId(), quiz.getDate(),
            quiz.getQuizType(), quiz.getTheme(), quiz.getQuestions()));
    }

    @PostMapping("/answer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuizResultResponse> answer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AnswerRequest req) {
        Couple couple = coupleService.getActiveCouple(principal.userId());

        // Verify quiz belongs to this couple
        DailyQuiz quiz = quizRepo.findById(req.quizId())
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));
        if (!quiz.getCoupleId().equals(couple.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        QuizAnswer answer = quizService.submitAnswer(
            principal.userId(), req.quizId(), couple.getId(), req.answers(), req.partnerGuess());

        String partnerAnswers = null;
        if (answer.getRevealed()) {
            partnerAnswers = answerRepo.findByDailyQuizId(req.quizId()).stream()
                .filter(a -> !a.getUserId().equals(principal.userId()))
                .map(QuizAnswer::getAnswers)
                .findFirst().orElse(null);
        }

        return ApiResponse.ok(new QuizResultResponse(
            answer.getScore() != null ? answer.getScore() : 0,
            answer.getRevealed(),
            answer.getAnswers(), partnerAnswers, quiz.getQuestions()));
    }

    @GetMapping("/result/{date}")
    public ApiResponse<QuizResultResponse> result(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String date) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        LocalDate d = LocalDate.parse(date);

        DailyQuiz quiz = quizRepo.findByCoupleIdAndDate(couple.getId(), d)
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));

        QuizAnswer myAnswer = answerRepo.findByDailyQuizIdAndUserId(quiz.getId(), principal.userId())
            .orElseThrow(() -> new BizException(ErrorCode.QUIZ_NOT_AVAILABLE));

        if (!myAnswer.getRevealed()) throw new BizException(ErrorCode.QUIZ_NOT_REVEALED);

        String partnerAnswers = answerRepo.findByDailyQuizId(quiz.getId()).stream()
            .filter(a -> !a.getUserId().equals(principal.userId()))
            .map(QuizAnswer::getAnswers)
            .findFirst().orElse(null);

        return ApiResponse.ok(new QuizResultResponse(
            myAnswer.getScore(), true, myAnswer.getAnswers(), partnerAnswers, quiz.getQuestions()));
    }

    @GetMapping("/history")
    public ApiResponse<CursorPage<QuizResponse>> history(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        Couple couple = coupleService.getActiveCouple(principal.userId());
        size = Math.min(size, 50);

        List<DailyQuiz> quizzes;
        if (cursor != null) {
            quizzes = quizRepo.findByCoupleIdAndIdLessThanOrderByIdDesc(
                couple.getId(), cursor, PageRequest.of(0, size + 1));
        } else {
            quizzes = quizRepo.findByCoupleIdOrderByIdDesc(
                couple.getId(), PageRequest.of(0, size + 1));
        }

        boolean hasMore = quizzes.size() > size;
        if (hasMore) quizzes = quizzes.subList(0, size);

        List<QuizResponse> data = quizzes.stream()
            .map(q -> new QuizResponse(q.getId(), q.getDate(), q.getQuizType(),
                q.getTheme(), q.getQuestions()))
            .toList();

        String nextCursor = hasMore ? quizzes.get(quizzes.size() - 1).getId().toString() : null;
        return ApiResponse.ok(new CursorPage<>(data, nextCursor, hasMore));
    }
}
