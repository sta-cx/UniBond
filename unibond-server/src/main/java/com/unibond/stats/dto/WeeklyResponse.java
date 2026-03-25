package com.unibond.stats.dto;
import java.util.List;
public record WeeklyResponse(List<DayScore> scores, double avgScore, int quizzesCompleted) {}
