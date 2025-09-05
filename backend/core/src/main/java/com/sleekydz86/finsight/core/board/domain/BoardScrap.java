package com.sleekydz86.finsight.core.board.domain;

import java.time.LocalDateTime;

public class BoardScrap {
    private final Long id;
    private final Long boardId;
    private final String userEmail;
    private final LocalDateTime scrapedAt;

    public BoardScrap() {
        this.id = null;
        this.boardId = null;
        this.userEmail = "";
        this.scrapedAt = LocalDateTime.now();
    }

    public BoardScrap(Long id, Long boardId, String userEmail, LocalDateTime scrapedAt) {
        this.id = id;
        this.boardId = boardId;
        this.userEmail = userEmail;
        this.scrapedAt = scrapedAt;
    }

    public Long getId() { return id; }
    public Long getBoardId() { return boardId; }
    public String getUserEmail() { return userEmail; }
    public LocalDateTime getScrapedAt() { return scrapedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardScrap that = (BoardScrap) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BoardScrap{" +
                "id=" + id +
                ", boardId=" + boardId +
                ", userEmail='" + userEmail + '\'' +
                ", scrapedAt=" + scrapedAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long boardId;
        private String userEmail;
        private LocalDateTime scrapedAt = LocalDateTime.now();

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

        public BoardScrap build() {
            return new BoardScrap(id, boardId, userEmail, scrapedAt);
        }
    }
}