package com.unibond.stats.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "achievements")
public class Achievement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long coupleId;
    private String type;
    private Instant unlockedAt;
    @PrePersist void prePersist() { if (unlockedAt == null) unlockedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long c) { this.coupleId = c; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public Instant getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(Instant u) { this.unlockedAt = u; }
}
