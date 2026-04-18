package com.unibond.stats.repository;
import com.unibond.stats.entity.DailyStats;
import com.unibond.stats.entity.DailyStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface DailyStatsRepository extends JpaRepository<DailyStats, DailyStatsId> {
    List<DailyStats> findByCoupleIdAndStatDateBetweenOrderByStatDateDesc(
        Long coupleId, LocalDate start, LocalDate end);
    Optional<DailyStats> findByCoupleIdAndStatDate(Long coupleId, LocalDate statDate);
    long countByCoupleIdAndMatchScoreGreaterThanEqual(Long coupleId, int minScore);
}
