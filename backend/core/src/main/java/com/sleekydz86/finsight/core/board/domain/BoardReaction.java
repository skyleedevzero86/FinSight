package com.sleekydz86.finsight.core.board.domain;

import com.sleekydz86.finsight.core.comment.domain.ReactionType;

import java.time.LocalDateTime;

public class BoardReaction {
    private final Long id;
    private final Long boardId;
    private final String userEmail;
    private final ReactionType reactionType;
    private final LocalDateTime createdAt;

    public BoardReaction() {
        this.id = null;
        this.boardId = null;
        this.userEmail = "";
        this.reactionType = ReactionType.LIKE;
        this.createdAt = LocalDateTime.now();
    }

    public BoardReaction(Long id, Long boardId, String userEmail, ReactionType reactionType, LocalDateTime createdAt) {
        this.id = id;
        this.boardId = boardId;
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
    public Long getBoardId() { return boardId; }
    public String getUserEmail() { return userEmail; }
    public ReactionType getReactionType() { return reactionType; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardReaction that = (BoardReaction) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BoardReaction{" +
                "id=" + id +
                ", boardId=" + boardId +
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
        private Long boardId;
        private String userEmail;
        private ReactionType reactionType;
        private LocalDateTime createdAt = LocalDateTime.now();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder boardId(Long boardId) {
            this.boardId = boardId;
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

        public BoardReaction build() {
            return new BoardReaction(id, boardId, userEmail, reactionType, createdAt);
        }
    }
}