package com.sleekydz86.finsight.core.board.domain;

import java.time.LocalDateTime;

public class BoardScrap {
    private Long id;
    private Long boardId;
    private String userEmail;
    private LocalDateTime scrapedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BoardScrap() {
    }

    public BoardScrap(Long id, Long boardId, String userEmail, LocalDateTime scrapedAt,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.boardId = boardId;
        this.userEmail = userEmail;
        this.scrapedAt = scrapedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long boardId;
        private String userEmail;
        private LocalDateTime scrapedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

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

        public Builder scrapedAt(LocalDateTime scrapedAt) {
            this.scrapedAt = scrapedAt;
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

        public BoardScrap build() {
            return new BoardScrap(id, boardId, userEmail, scrapedAt, createdAt, updatedAt);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getBoardId() {
        return boardId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public LocalDateTime getScrapedAt() {
        return scrapedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "BoardScrap{" +
                "id=" + id +
                ", boardId=" + boardId +
                ", userEmail='" + userEmail + '\'' +
                ", scrapedAt=" + scrapedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}