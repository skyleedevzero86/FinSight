package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;

import java.time.LocalDateTime;
import java.util.List;

public class BoardListResponse {
    private final Long id;
    private final String title;
    private final String authorEmail;
    private final BoardType boardType;
    private final BoardStatus status;
    private final int viewCount;
    private final int likeCount;
    private final int dislikeCount;
    private final int commentCount;
    private final List<String> hashtags;
    private final String timeAgo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public BoardListResponse() {
        this.id = null;
        this.title = "";
        this.authorEmail = "";
        this.boardType = BoardType.COMMUNITY;
        this.status = BoardStatus.ACTIVE;
        this.viewCount = 0;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.commentCount = 0;
        this.hashtags = List.of();
        this.timeAgo = "";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public BoardListResponse(Long id, String title, String authorEmail, BoardType boardType,
                             BoardStatus status, int viewCount, int likeCount, int dislikeCount,
                             int commentCount, List<String> hashtags, String timeAgo,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.authorEmail = authorEmail;
        this.boardType = boardType;
        this.status = status;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.commentCount = commentCount;
        this.hashtags = hashtags != null ? hashtags : List.of();
        this.timeAgo = timeAgo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static BoardListResponse from(Board board) {
        return new BoardListResponse(
                board.getId(),
                board.getTitle(),
                board.getAuthorEmail(),
                board.getBoardType(),
                board.getStatus(),
                board.getViewCount(),
                board.getLikeCount(),
                board.getDislikeCount(),
                board.getCommentCount(),
                board.getHashtags(),
                calculateTimeAgo(board.getCreatedAt()),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }

    private static String calculateTimeAgo(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(createdAt, now).toMinutes();

        if (minutes < 1) {
            return "방금 전";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (minutes < 1440) {
            return (minutes / 60) + "시간 전";
        } else if (minutes < 10080) {
            return (minutes / 1440) + "일 전";
        } else if (minutes < 43200) {
            return (minutes / 10080) + "주 전";
        } else {
            return (minutes / 43200) + "달 전";
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthorEmail() { return authorEmail; }
    public BoardType getBoardType() { return boardType; }
    public BoardStatus getStatus() { return status; }
    public int getViewCount() { return viewCount; }
    public int getLikeCount() { return likeCount; }
    public int getDislikeCount() { return dislikeCount; }
    public int getCommentCount() { return commentCount; }
    public List<String> getHashtags() { return hashtags; }
    public String getTimeAgo() { return timeAgo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "BoardListResponse{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", boardType=" + boardType +
                ", status=" + status +
                ", viewCount=" + viewCount +
                ", likeCount=" + likeCount +
                ", commentCount=" + commentCount +
                ", timeAgo='" + timeAgo + '\'' +
                '}';
    }
}