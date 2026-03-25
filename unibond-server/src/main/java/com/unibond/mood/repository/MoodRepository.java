package com.unibond.mood.repository;
import com.unibond.mood.entity.MoodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MoodRepository extends JpaRepository<MoodStatus, Long> {
    Optional<MoodStatus> findTopByUserIdOrderByUpdatedAtDesc(Long userId);
}
