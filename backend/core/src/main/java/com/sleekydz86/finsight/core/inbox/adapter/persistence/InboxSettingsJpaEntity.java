package com.sleekydz86.finsight.core.inbox.adapter.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbox_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxSettingsJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "youtube_enabled", nullable = false)
    private boolean youtubeEnabled = true;

    @Column(name = "news_enabled", nullable = false)
    private boolean newsEnabled = true;

    @Column(name = "comment_enabled", nullable = false)
    private boolean commentEnabled = true;

    @Column(name = "qna_enabled", nullable = false)
    private boolean qnaEnabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public InboxSettingsJpaEntity(
            Long userId,
            boolean youtubeEnabled,
            boolean newsEnabled,
            boolean commentEnabled,
            boolean qnaEnabled,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.userId = userId;
        this.youtubeEnabled = youtubeEnabled;
        this.newsEnabled = newsEnabled;
        this.commentEnabled = commentEnabled;
        this.qnaEnabled = qnaEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InboxSettingsJpaEntity defaults(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return InboxSettingsJpaEntity.builder()
                .userId(userId)
                .youtubeEnabled(true)
                .newsEnabled(true)
                .commentEnabled(true)
                .qnaEnabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void update(boolean youtube, boolean news, boolean comment, boolean qna) {
        this.youtubeEnabled = youtube;
        this.newsEnabled = news;
        this.commentEnabled = comment;
        this.qnaEnabled = qna;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
