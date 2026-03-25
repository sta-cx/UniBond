package com.unibond.quiz.dto;
import com.unibond.quiz.entity.QuizType;
import java.time.LocalDate;
public record QuizResponse(Long id, LocalDate date, QuizType quizType,
    String theme, String questions) {}
