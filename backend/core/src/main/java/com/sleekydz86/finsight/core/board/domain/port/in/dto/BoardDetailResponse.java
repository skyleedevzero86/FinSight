package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardFile;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;

import java.time.LocalDateTime;
import java.util.List;

public class BoardDetailResponse {
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
    private final BoardNavigationResponse navigation;

    public BoardDetailResponse() {
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
        this.hashtags = List.of();
        this.files = List.of();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.navigation = null;
    }

    public BoardDetailResponse(Long id, String title, String content, String authorEmail,
                               BoardType boardType, BoardStatus status, int viewCount,
                               int likeCount, int dislikeCount, int commentCount, int reportCount,
                               List<String> hashtags, List<BoardFile> files,
                               LocalDateTime createdAt, LocalDateTime updatedAt,
                               BoardNavigationResponse navigation) {
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
        this.hashtags = hashtags != null ? hashtags : List.of();
        this.files = files != null ? files : List.of();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.navigation = navigation;
}

public static BoardDetailResponse from(Board board) {
    return new BoardDetailResponse(
            board.getId(),
            board.getTitle(),
            board.getContent(),
            board.getAuthorEmail(),
            board.getBoardType(),
            board.getStatus(),
            board.getViewCount(),
            board.getLikeCount(),
            board.getDislikeCount(),
            board.getCommentCount(),
            board.getReportCount(),
            board.getHashtags(),
            board.getFiles(),
            board.getCreatedAt(),
            board.getUpdatedAt(),
            null
    );
}

public static BoardDetailResponse from(Board board, BoardNavigationResponse navigation) {
    return new BoardDetailResponse(
            board.getId(),
            board.getTitle(),
            board.getContent(),
            board.getAuthorEmail(),
            board.getBoardType(),
            board.getStatus(),
            board.getViewCount(),
            board.getLikeCount(),
            board.getDislikeCount(),
            board.getCommentCount(),
            board.getReportCount(),
            board.getHashtags(),
            board.getFiles(),
            board.getCreatedAt(),
            board.getUpdatedAt(),
            navigation
    );
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
public BoardNavigationResponse getNavigation() { return navigation; }

@Override
public String toString() {
    return "BoardDetailResponse{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", authorEmail='" + authorEmail + '\'' +
            ", boardType=" + boardType +
            ", status=" + status +
            ", viewCount=" + viewCount +
            ", likeCount=" + likeCount +
            ", commentCount=" + commentCount +
            '}';
  }
}