package com.unibond.quiz.dto;
import java.util.List;
public record QuizResultResponse(int score, boolean revealed,
    String myAnswers, String partnerAnswers, String questions) {}
