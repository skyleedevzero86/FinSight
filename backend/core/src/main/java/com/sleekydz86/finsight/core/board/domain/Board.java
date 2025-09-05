package com.sleekydz86.finsight.core.board.domain;

import com.sleekydz86.finsight.core.global.BaseEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Board {
    private final Long id;
    private final String title;
    private final String content;
    private final String authorEmail;
    private final BoardType boardType;
    private final BoardStatus status;
    private final int viewCount;
    private final int likeCount;
    private final int dislikeCount;
    private final int commentCount;
    private final int reportCount;
    private final List<String> hashtags;
    private final List<BoardFile> files;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<Board> replies;

    public Board() {
        this.id = null;
        this.title = "";
        this.content = "";
        this.authorEmail = "";
        this.boardType = BoardType.COMMUNITY;
        this.status = BoardStatus.ACTIVE;
        this.viewCount = 0;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.commentCount = 0;
        this.reportCount = 0;
        this.hashtags = new ArrayList<>();
        this.files = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.replies = new ArrayList<>();
    }

    public Board(Long id, String title, String content, String authorEmail, BoardType boardType,
                 BoardStatus status, int viewCount, int likeCount, int dislikeCount,
                 int commentCount, int reportCount, List<String> hashtags, List<BoardFile> files,
                 LocalDateTime createdAt, LocalDateTime updatedAt, List<Board> replies) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorEmail = authorEmail;
        this.boardType = boardType;
        this.status = status;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.commentCount = commentCount;
        this.reportCount = reportCount;
        this.hashtags = hashtags != null ? hashtags : new ArrayList<>();
        this.files = files != null ? files : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.replies = replies != null ? replies : new ArrayList<>();
    }

    public Board updateContent(String title, String content, List<String> hashtags) {
        return new Board(this.id, title, content, this.authorEmail, this.boardType,
                this.status, this.viewCount, this.likeCount, this.dislikeCount,
                this.commentCount, this.reportCount, hashtags, this.files,
                this.createdAt, LocalDateTime.now(), this.replies);
    }

    public Board updateStatus(BoardStatus newStatus) {
        return new Board(this.id, this.title, this.content, this.authorEmail, this.boardType,
                newStatus, this.viewCount, this.likeCount, this.dislikeCount,
                this.commentCount, this.reportCount, this.hashtags, this.files,
                this.createdAt, LocalDateTime.now(), this.replies);
    }

    public Board incrementView() {
        return new Board(this.id, this.title, this.content, this.authorEmail, this.boardType,
                this.status, this.viewCount + 1, this.likeCount, this.dislikeCount,
                this.commentCount, this.reportCount, this.hashtags, this.files,
                this.createdAt, this.updatedAt, this.replies);
    }

    public Board incrementLike() {
        return new Board(this.id, this.title, this.content, this.authorEmail, this.boardType,
                this.status, this.viewCount, this.likeCount + 1, this.dislikeCount,
                this.commentCount, this.reportCount, this.hashtags, this.files,
                this.createdAt, this.updatedAt, this.replies);
    }

    public Board incrementDislike() {
        return new Board(this.id, this.title, this.content, this.authorEmail, this.boardType,
                this.status, this.viewCount, this.likeCount, this.dislikeCount + 1,
                this.commentCount, this.reportCount, this.hashtags, this.files,
                this.createdAt, this.updatedAt, this.replies);
    }

    public Board incrementComment() {
        return new Board(this.id, this.title, this.content, this.authorEmail, this.boardType,
                this.status, this.viewCount, this.likeCount, this.dislikeCount,
                this.commentCount + 1, this.reportCount, this.hashtags, this.files,
                this.createdAt, this.updatedAt, this.replies);
    }

    public Board incrementReport() {
        return new Board(this.id, this.title, this.content, this.authorEmail, this.boardType,
                this.status, this.viewCount, this.likeCount, this.dislikeCount,
                this.commentCount, this.reportCount + 1, this.hashtags, this.files,
                this.createdAt, this.updatedAt, this.replies);
    }

    public Board updateCommentCount(int commentCount) {
        return new Board(this.id, this.title, this.content, this.authorEmail, this.boardType,
                this.status, this.viewCount, this.likeCount, this.dislikeCount,
                commentCount, this.reportCount, this.hashtags, this.files,
                this.createdAt, this.updatedAt, this.replies);
    }

    public boolean isActive() {
        return status == BoardStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return status == BoardStatus.DELETED;
    }

    public boolean isBlocked() {
        return status == BoardStatus.BLOCKED;
    }

    public boolean isReported() {
        return status == BoardStatus.REPORTED;
    }

    public boolean isHidden() {
        return status == BoardStatus.HIDDEN;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthorEmail() { return authorEmail; }
    public BoardType getBoardType() { return boardType; }
    public BoardStatus getStatus() { return status; }
    public int getViewCount() { return viewCount; }
    public int getLikeCount() { return likeCount; }
    public int getDislikeCount() { return dislikeCount; }
    public int getCommentCount() { return commentCount; }
    public int getReportCount() { return reportCount; }
    public List<String> getHashtags() { return hashtags; }
    public List<BoardFile> getFiles() { return files; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<Board> getReplies() { return replies; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return id != null && id.equals(board.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Board{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", boardType=" + boardType +
                ", status=" + status +
                ", viewCount=" + viewCount +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", createdAt=" + createdAt +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String title;
        private String content;
        private String authorEmail;
        private BoardType boardType = BoardType.COMMUNITY;
        private BoardStatus status = BoardStatus.ACTIVE;
        private int viewCount = 0;
        private int likeCount = 0;
        private int dislikeCount = 0;
        private int commentCount = 0;
        private int reportCount = 0;
        private List<String> hashtags = new ArrayList<>();
        private List<BoardFile> files = new ArrayList<>();
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private List<Board> replies = new ArrayList<>();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder authorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
            return this;
        }

        public Builder boardType(BoardType boardType) {
            this.boardType = boardType;
            return this;
        }

        public Builder status(BoardStatus status) {
            this.status = status;
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

        public Builder reportCount(int reportCount) {
            this.reportCount = reportCount;
            return this;
        }

        public Builder hashtags(List<String> hashtags) {
            this.hashtags = hashtags != null ? hashtags : new ArrayList<>();
            return this;
        }

        public Builder files(List<BoardFile> files) {
            this.files = files != null ? files : new ArrayList<>();
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

        public Builder replies(List<Board> replies) {
            this.replies = replies != null ? replies : new ArrayList<>();
            return this;
        }

        public Board build() {
            return new Board(id, title, content, authorEmail, boardType, status,
                    viewCount, likeCount, dislikeCount, commentCount, reportCount,
                    hashtags, files, createdAt, updatedAt, replies);
        }
    }
}