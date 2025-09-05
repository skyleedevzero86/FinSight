package com.sleekydz86.finsight.core.comment.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Comment {
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
    private final List<Comment> replies;

    public Comment() {
        this.id = null;
        this.content = null;
        this.authorEmail = null;
        this.commentType = null;
        this.targetId = null;
        this.parentId = null;
        this.status = CommentStatus.ACTIVE;
        this.likeCount = 0;
        this.dislikeCount = 0;
        this.reportCount = 0;
        this.createdAt = null;
        this.updatedAt = null;
        this.replies = new ArrayList<>();
    }

    public Comment(Long id, String content, String authorEmail, CommentType commentType,
            Long targetId, Long parentId, CommentStatus status, int likeCount,
            int dislikeCount, int reportCount, LocalDateTime createdAt,
            LocalDateTime updatedAt, List<Comment> replies) {
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
        this.replies = replies != null ? replies : new ArrayList<>();
    }

    public Comment addReply(Comment reply) {
        List<Comment> newReplies = new ArrayList<>(this.replies);
        newReplies.add(reply);
        return new Comment(this.id, this.content, this.authorEmail, this.commentType,
                this.targetId, this.parentId, this.status, this.likeCount,
                this.dislikeCount, this.reportCount, this.createdAt, this.updatedAt, newReplies);
    }

    public Comment updateContent(String newContent) {
        return new Comment(this.id, newContent, this.authorEmail, this.commentType,
                this.targetId, this.parentId, this.status, this.likeCount,
                this.dislikeCount, this.reportCount, this.createdAt, this.updatedAt, this.replies);
    }

    public Comment updateStatus(CommentStatus newStatus) {
        return new Comment(this.id, this.content, this.authorEmail, this.commentType,
                this.targetId, this.parentId, newStatus, this.likeCount,
                this.dislikeCount, this.reportCount, this.createdAt, this.updatedAt, this.replies);
    }

    public Comment incrementLike() {
        return new Comment(this.id, this.content, this.authorEmail, this.commentType,
                this.targetId, this.parentId, this.status, this.likeCount + 1,
                this.dislikeCount, this.reportCount, this.createdAt, this.updatedAt, this.replies);
    }

    public Comment incrementDislike() {
        return new Comment(this.id, this.content, this.authorEmail, this.commentType,
                this.targetId, this.parentId, this.status, this.likeCount,
                this.dislikeCount + 1, this.reportCount, this.createdAt, this.updatedAt, this.replies);
    }

    public Comment incrementReport() {
        return new Comment(this.id, this.content, this.authorEmail, this.commentType,
                this.targetId, this.parentId, this.status, this.likeCount,
                this.dislikeCount, this.reportCount + 1, this.createdAt, this.updatedAt, this.replies);
    }

    public boolean isReply() {
        return this.parentId != null;
    }

    public boolean isActive() {
        return this.status == CommentStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == CommentStatus.DELETED;
    }

    public boolean isBlocked() {
        return this.status == CommentStatus.BLOCKED;
    }

    public boolean isReported() {
        return this.status == CommentStatus.REPORTED;
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

    public List<Comment> getReplies() {
        return replies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Comment comment = (Comment) o;
        return Objects.equals(id, comment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Comment{" +
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
                ", replies=" + replies.size() + " replies" +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String content;
        private String authorEmail;
        private CommentType commentType;
        private Long targetId;
        private Long parentId;
        private CommentStatus status = CommentStatus.ACTIVE;
        private int likeCount = 0;
        private int dislikeCount = 0;
        private int reportCount = 0;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private List<Comment> replies = new ArrayList<>();

        public Builder id(Long id) {
            this.id = id;
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

        public Builder commentType(CommentType commentType) {
            this.commentType = commentType;
            return this;
        }

        public Builder targetId(Long targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder status(CommentStatus status) {
            this.status = status;
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

        public Builder reportCount(int reportCount) {
            this.reportCount = reportCount;
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

        public Builder replies(List<Comment> replies) {
            this.replies = replies != null ? replies : new ArrayList<>();
            return this;
        }

        public Comment build() {
            return new Comment(id, content, authorEmail, commentType, targetId, parentId,
                    status, likeCount, dislikeCount, reportCount, createdAt, updatedAt, replies);
        }
    }
}