package com.unibond.quiz.repository;
import com.unibond.quiz.entity.QuestionPool;
import com.unibond.quiz.entity.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface QuestionPoolRepository extends JpaRepository<QuestionPool, Long> {
    @Query("SELECT q FROM QuestionPool q WHERE q.quizType = :type ORDER BY q.usedCount ASC, FUNCTION('RANDOM') LIMIT 5")
    List<QuestionPool> findLeastUsedByType(QuizType type);
}
