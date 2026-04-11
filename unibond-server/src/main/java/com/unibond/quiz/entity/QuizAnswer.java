package com.unibond.quiz.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "quiz_answers")
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "daily_quiz_id")
    private Long dailyQuizId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "couple_id")
    private Long coupleId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String answers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "partner_guess")
    private String partnerGuess;

    private Integer score;
    @Column(name = "completed_at")
    private Instant completedAt;
    private Boolean revealed;

    @PrePersist
    void prePersist() {
        if (completedAt == null) completedAt = Instant.now();
        if (revealed == null) revealed = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDailyQuizId() { return dailyQuizId; }
    public void setDailyQuizId(Long dailyQuizId) { this.dailyQuizId = dailyQuizId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long coupleId) { this.coupleId = coupleId; }
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    public String getPartnerGuess() { return partnerGuess; }
    public void setPartnerGuess(String partnerGuess) { this.partnerGuess = partnerGuess; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Boolean getRevealed() { return revealed; }
    public void setRevealed(Boolean revealed) { this.revealed = revealed; }
}
