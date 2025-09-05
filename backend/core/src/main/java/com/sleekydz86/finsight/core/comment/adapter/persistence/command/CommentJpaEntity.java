package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.global.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "comments")
public class CommentJpaEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_email", nullable = false)
    private String authorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", nullable = false)
    private CommentType commentType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "parent_id")
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CommentStatus status = CommentStatus.ACTIVE;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    private int dislikeCount = 0;

    @Column(name = "report_count", nullable = false)
    private int reportCount = 0;

    public CommentJpaEntity() {
    }

    public CommentJpaEntity(Long id, String content, String authorEmail, CommentType commentType,
            Long targetId, Long parentId, CommentStatus status, int likeCount,
            int dislikeCount, int reportCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public CommentType getCommentType() {
        return commentType;
    }

    public void setCommentType(CommentType commentType) {
        this.commentType = commentType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public CommentStatus getStatus() {
        return status;
    }

    public void setStatus(CommentStatus status) {
        this.status = status;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    public void setDislikeCount(int dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public int getReportCount() {
        return reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CommentJpaEntity that = (CommentJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CommentJpaEntity{" +
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
                '}';
    }
}