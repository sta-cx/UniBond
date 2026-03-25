package com.unibond.couple.repository;

import com.unibond.couple.entity.Couple;
import com.unibond.couple.entity.CoupleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CoupleRepository extends JpaRepository<Couple, Long> {
    @Query("SELECT c FROM Couple c WHERE (c.userAId = :userId OR c.userBId = :userId) AND c.status = :status")
    Optional<Couple> findActiveByUserId(Long userId, CoupleStatus status);

    default Optional<Couple> findActiveByUserId(Long userId) {
        return findActiveByUserId(userId, CoupleStatus.ACTIVE);
    }

    List<Couple> findByStatus(CoupleStatus status);
}
