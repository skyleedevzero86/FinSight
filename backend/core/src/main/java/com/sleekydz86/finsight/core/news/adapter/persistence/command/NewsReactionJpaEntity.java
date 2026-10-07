package com.sleekydz86.finsight.core.news.adapter.persistence.command;

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
        name = "news_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_email", "news_id"})
)
public class NewsReactionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "reaction_type", nullable = false, length = 16)
    private String reactionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected NewsReactionJpaEntity() {
    }

    public NewsReactionJpaEntity(Long newsId, String userEmail, String reactionType) {
        this.newsId = newsId;
        this.userEmail = userEmail;
        this.reactionType = reactionType;
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

    public Long getNewsId() {
        return newsId;
    }

    public String getReactionType() {
        return reactionType;
    }

    public void setReactionType(String reactionType) {
        this.reactionType = reactionType;
    }
}
