package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_vod_comments")
public class LiveVodCommentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false, length = 32)
    private String videoId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "author_nickname", length = 100)
    private String authorNickname;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private LiveVodCommentJpaEntity parent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected LiveVodCommentJpaEntity() {
    }

    public LiveVodCommentJpaEntity(
            String videoId,
            String userEmail,
            String authorNickname,
            String content,
            LiveVodCommentJpaEntity parent) {
        this.videoId = videoId;
        this.userEmail = userEmail;
        this.authorNickname = authorNickname;
        this.content = content;
        this.parent = parent;
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

    public String getAuthorNickname() {
        return authorNickname;
    }

    public String getContent() {
        return content;
    }

    public LiveVodCommentJpaEntity getParent() {
        return parent;
    }

    public Long getParentId() {
        return parent == null ? null : parent.getId();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
