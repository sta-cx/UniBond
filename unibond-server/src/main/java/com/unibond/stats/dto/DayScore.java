package com.unibond.stats.dto;
import java.time.LocalDate;
public record DayScore(LocalDate date, int score, String quizType) {}
