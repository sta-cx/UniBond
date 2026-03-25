package com.unibond.quiz.dto;
import jakarta.validation.constraints.NotNull;
public record AnswerRequest(@NotNull Long quizId, @NotNull String answers, String partnerGuess) {}
