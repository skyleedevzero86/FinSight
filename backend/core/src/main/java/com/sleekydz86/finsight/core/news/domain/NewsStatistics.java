package com.sleekydz86.finsight.core.news.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class NewsStatistics {
    private final Long id;
    private final Long newsId;
    private final int viewCount;
    private final int likeCount;
    private final int dislikeCount;
    private final int commentCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public NewsStatistics() {
        this.id = null;
        this.newsId = null;
        this.viewCount = 0;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.commentCount = 0;
        this.createdAt = null;
        this.updatedAt = null;
    }

    public NewsStatistics(Long id, Long newsId, int viewCount, int likeCount,
                          int dislikeCount, int commentCount, LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.id = id;
        this.newsId = newsId;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public NewsStatistics incrementView() {
        return new NewsStatistics(this.id, this.newsId, this.viewCount + 1,
                this.likeCount, this.dislikeCount, this.commentCount,
                this.createdAt, LocalDateTime.now());
    }

    public NewsStatistics incrementLike() {
        return new NewsStatistics(this.id, this.newsId, this.viewCount,
                this.likeCount + 1, this.dislikeCount, this.commentCount,
                this.createdAt, LocalDateTime.now());
    }

    public NewsStatistics incrementDislike() {
        return new NewsStatistics(this.id, this.newsId, this.viewCount,
                this.likeCount, this.dislikeCount + 1, this.commentCount,
                this.createdAt, LocalDateTime.now());
    }

    public NewsStatistics updateCommentCount(int commentCount) {
        return new NewsStatistics(this.id, this.newsId, this.viewCount,
                this.likeCount, this.dislikeCount, commentCount,
                this.createdAt, LocalDateTime.now());
    }

    public Long getId() { return id; }
    public Long getNewsId() { return newsId; }
    public int getViewCount() { return viewCount; }
    public int getLikeCount() { return likeCount; }
    public int getDislikeCount() { return dislikeCount; }
    public int getCommentCount() { return commentCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsStatistics that = (NewsStatistics) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "NewsStatistics{" +
                "id=" + id +
                ", newsId=" + newsId +
                ", viewCount=" + viewCount +
                ", likeCount=" + likeCount +
                ", dislikeCount=" + dislikeCount +
                ", commentCount=" + commentCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long newsId;
        private int viewCount = 0;
        private int likeCount = 0;
        private int dislikeCount = 0;
        private int commentCount = 0;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder newsId(Long newsId) {
            this.newsId = newsId;
            return this;
        }

        public Builder viewCount(int viewCount) {
            this.viewCount = viewCount;
            return this;
        }

        public Builder likeCount(int likeCount) {
            this.likeCount = likeCount;
            return this;
        }

        public Builder dislikeCount(int dislikeCount) {
            this.dislikeCount = dislikeCount;
            return this;
        }

        public Builder commentCount(int commentCount) {
            this.commentCount = commentCount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public NewsStatistics build() {
            return new NewsStatistics(id, newsId, viewCount, likeCount, dislikeCount, commentCount, createdAt, updatedAt);
        }
    }
}