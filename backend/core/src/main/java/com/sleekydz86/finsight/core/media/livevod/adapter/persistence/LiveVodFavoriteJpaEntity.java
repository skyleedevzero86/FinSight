package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "live_vod_favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_email", "video_id"})
)
public class LiveVodFavoriteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 32)
    private String videoId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected LiveVodFavoriteJpaEntity() {
    }

    public LiveVodFavoriteJpaEntity(String videoId, String userEmail) {
        this.videoId = videoId;
        this.userEmail = userEmail;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
