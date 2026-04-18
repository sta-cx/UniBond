package com.unibond.couple.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "couples")
public class Couple {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_a_id")
    private Long userAId;
    @Column(name = "user_b_id")
    private Long userBId;
    @Column(name = "anniversary_date")
    private LocalDate anniversaryDate;
    @Column(name = "bind_at")
    private Instant bindAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private CoupleStatus status;

    @PrePersist
    void prePersist() {
        if (bindAt == null) bindAt = Instant.now();
        if (status == null) status = CoupleStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserAId() { return userAId; }
    public void setUserAId(Long userAId) { this.userAId = userAId; }
    public Long getUserBId() { return userBId; }
    public void setUserBId(Long userBId) { this.userBId = userBId; }
    public LocalDate getAnniversaryDate() { return anniversaryDate; }
    public void setAnniversaryDate(LocalDate d) { this.anniversaryDate = d; }
    public Instant getBindAt() { return bindAt; }
    public void setBindAt(Instant bindAt) { this.bindAt = bindAt; }
    public CoupleStatus getStatus() { return status; }
    public void setStatus(CoupleStatus status) { this.status = status; }
}
