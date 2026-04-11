package com.unibond.mood.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mood_status")
public class MoodStatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "couple_id")
    private Long coupleId;
    @Column(name = "mood_emoji")
    private String moodEmoji;
    @Column(name = "mood_text")
    private String moodText;
    @Column(name = "updated_at")
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
