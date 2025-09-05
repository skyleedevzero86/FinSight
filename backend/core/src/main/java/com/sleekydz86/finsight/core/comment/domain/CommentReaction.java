package com.sleekydz86.finsight.core.comment.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class CommentReaction {
    private final Long id;
    private final Long commentId;
    private final String userEmail;
    private final ReactionType reactionType;
    private final LocalDateTime createdAt;

    public CommentReaction() {
        this.id = null;
        this.commentId = null;
        this.userEmail = null;
        this.reactionType = null;
        this.createdAt = null;
    }

    public CommentReaction(Long id, Long commentId, String userEmail, ReactionType reactionType, LocalDateTime createdAt) {
        this.id = id;
        this.commentId = commentId;
        this.userEmail = userEmail;
        this.reactionType = reactionType;
        this.createdAt = createdAt;
    }

    public boolean isLike() {
        return reactionType == ReactionType.LIKE;
    }

    public boolean isDislike() {
        return reactionType == ReactionType.DISLIKE;
    }

    public Long getId() { return id; }
    public Long getCommentId() { return commentId; }
    public String getUserEmail() { return userEmail; }
    public ReactionType getReactionType() { return reactionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentReaction that = (CommentReaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CommentReaction{" +
                "id=" + id +
                ", commentId=" + commentId +
                ", userEmail='" + userEmail + '\'' +
                ", reactionType=" + reactionType +
                ", createdAt=" + createdAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long commentId;
        private String userEmail;
        private ReactionType reactionType;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder commentId(Long commentId) {
            this.commentId = commentId;
            return this;
        }

        public Builder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder reactionType(ReactionType reactionType) {
            this.reactionType = reactionType;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public CommentReaction build() {
            return new CommentReaction(id, commentId, userEmail, reactionType, createdAt);
        }
    }
}
