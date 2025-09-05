package com.sleekydz86.finsight.core.comment.domain.port.in.dto;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CommentResponse {
    private final Long id;
    private final String content;
    private final String authorEmail;
    private final CommentType commentType;
    private final Long targetId;
    private final Long parentId;
    private final CommentStatus status;
    private final int likeCount;
    private final int dislikeCount;
    private final int reportCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<CommentResponse> replies;

    public CommentResponse() {
        this.id = null;
        this.content = null;
        this.authorEmail = null;
        this.commentType = null;
        this.targetId = null;
        this.parentId = null;
        this.status = null;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.reportCount = 0;
        this.createdAt = null;
        this.updatedAt = null;
        this.replies = List.of();
    }

    public CommentResponse(Long id, String content, String authorEmail, CommentType commentType,
            Long targetId, Long parentId, CommentStatus status, int likeCount,
            int dislikeCount, int reportCount, LocalDateTime createdAt,
            LocalDateTime updatedAt, List<CommentResponse> replies) {
        this.id = id;
        this.content = content;
        this.authorEmail = authorEmail;
        this.commentType = commentType;
        this.targetId = targetId;
        this.parentId = parentId;
        this.status = status;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.reportCount = reportCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.replies = replies != null ? replies : List.of();
    }

    public static CommentResponse from(Comment comment) {
        if (comment == null) {
            return new CommentResponse();
        }

        List<CommentResponse> replies = comment.getReplies().stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());

        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getAuthorEmail(),
                comment.getCommentType(),
                comment.getTargetId(),
                comment.getParentId(),
                comment.getStatus(),
                comment.getLikeCount(),
                comment.getDislikeCount(),
                comment.getReportCount(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies);
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public CommentType getCommentType() {
        return commentType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Long getParentId() {
        return parentId;
    }

    public CommentStatus getStatus() {
        return status;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public int getReportCount() {
        return reportCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<CommentResponse> getReplies() {
        return replies;
    }

    @Override
    public String toString() {
        return "CommentResponse{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", commentType=" + commentType +
                ", targetId=" + targetId +
                ", parentId=" + parentId +
                ", status=" + status +
                ", likeCount=" + likeCount +
                ", dislikeCount=" + dislikeCount +
                ", reportCount=" + reportCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", replies=" + replies +
                '}';
    }
}