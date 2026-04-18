package com.unibond.quiz.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "question_pool")
public class QuestionPool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "quiz_type")
    private QuizType quizType;

    private String question;

    @JdbcTypeCode(SqlTypes.JSON)
    private String options;

    private int difficulty;
    @Column(name = "used_count")
    private int usedCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public QuizType getQuizType() { return quizType; }
    public void setQuizType(QuizType t) { this.quizType = t; }
    public String getQuestion() { return question; }
    public void setQuestion(String q) { this.question = q; }
    public String getOptions() { return options; }
    public void setOptions(String o) { this.options = o; }
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int d) { this.difficulty = d; }
    public int getUsedCount() { return usedCount; }
    public void setUsedCount(int c) { this.usedCount = c; }
}
