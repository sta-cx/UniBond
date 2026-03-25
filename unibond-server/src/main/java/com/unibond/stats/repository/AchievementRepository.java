package com.unibond.stats.repository;
import com.unibond.stats.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByCoupleId(Long coupleId);
    boolean existsByCoupleIdAndType(Long coupleId, String type);
}
