package com.unibond.quiz.repository;
import com.unibond.quiz.entity.DailyQuiz;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface DailyQuizRepository extends JpaRepository<DailyQuiz, Long> {
    Optional<DailyQuiz> findByCoupleIdAndDate(Long coupleId, LocalDate date);
    List<DailyQuiz> findByCoupleIdAndIdLessThanOrderByIdDesc(Long coupleId, Long cursor, org.springframework.data.domain.Pageable pageable);
    List<DailyQuiz> findByCoupleIdOrderByIdDesc(Long coupleId, org.springframework.data.domain.Pageable pageable);
}
