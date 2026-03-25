package com.unibond.mood.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mood_status")
public class MoodStatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long coupleId;
    private String moodEmoji;
    private String moodText;
    private Instant updatedAt;

    @PrePersist @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCoupleId() { return coupleId; }
    public void setCoupleId(Long coupleId) { this.coupleId = coupleId; }
    public String getMoodEmoji() { return moodEmoji; }
    public void setMoodEmoji(String e) { this.moodEmoji = e; }
    public String getMoodText() { return moodText; }
    public void setMoodText(String t) { this.moodText = t; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant u) { this.updatedAt = u; }
}
