package com.unibond.quiz.repository;
import com.unibond.quiz.entity.QuizAnswer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    Optional<QuizAnswer> findByDailyQuizIdAndUserId(Long dailyQuizId, Long userId);
    List<QuizAnswer> findByDailyQuizId(Long dailyQuizId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(a) FROM QuizAnswer a WHERE a.dailyQuizId = :quizId")
    long countByDailyQuizIdForUpdate(Long quizId);
}
