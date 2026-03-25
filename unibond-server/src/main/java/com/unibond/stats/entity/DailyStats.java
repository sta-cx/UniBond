package com.unibond.stats.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name = "daily_stats")
@IdClass(DailyStatsId.class)
public class DailyStats {
    @Id private Long coupleId;
    @Id @Column(name = "stat_date") private LocalDate statDate;
    private int matchScore;
    private int streakDays;
    @Enumerated(EnumType.STRING)
    private com.unibond.quiz.entity.QuizType quizTypePlayed;

    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long c) { this.coupleId = c; }
    public LocalDate getStatDate() { return statDate; }
    public void setStatDate(LocalDate d) { this.statDate = d; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int s) { this.matchScore = s; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int s) { this.streakDays = s; }
    public com.unibond.quiz.entity.QuizType getQuizTypePlayed() { return quizTypePlayed; }
    public void setQuizTypePlayed(com.unibond.quiz.entity.QuizType t) { this.quizTypePlayed = t; }
}
