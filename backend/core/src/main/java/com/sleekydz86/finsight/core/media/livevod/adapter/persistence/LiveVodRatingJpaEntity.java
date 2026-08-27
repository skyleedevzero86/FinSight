package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "live_vod_ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_email", "video_id"})
)
public class LiveVodRatingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 32)
    private String videoId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "stars", nullable = false)
    private int stars;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LiveVodRatingJpaEntity() {
    }

    public LiveVodRatingJpaEntity(String videoId, String userEmail, int stars) {
        this.videoId = videoId;
        this.userEmail = userEmail;
        this.stars = stars;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }
}
