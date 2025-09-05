package com.sleekydz86.finsight.core.comment.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class CommentReport {
    private final Long id;
    private final Long commentId;
    private final String reporterEmail;
    private final String reason;
    private final String description;
    private final LocalDateTime reportedAt;
    private final boolean isProcessed;

    public CommentReport() {
        this.id = null;
        this.commentId = null;
        this.reporterEmail = null;
        this.reason = null;
        this.description = null;
        this.reportedAt = null;
        this.isProcessed = false;
    }

    public CommentReport(Long id, Long commentId, String reporterEmail, String reason,
                         String description, LocalDateTime reportedAt, boolean isProcessed) {
        this.id = id;
        this.commentId = commentId;
        this.reporterEmail = reporterEmail;
        this.reason = reason;
        this.description = description;
        this.reportedAt = reportedAt;
        this.isProcessed = isProcessed;
    }

    public CommentReport markAsProcessed() {
        return new CommentReport(this.id, this.commentId, this.reporterEmail, this.reason,
                this.description, this.reportedAt, true);
    }

    public Long getId() { return id; }
    public Long getCommentId() { return commentId; }
    public String getReporterEmail() { return reporterEmail; }
    public String getReason() { return reason; }
    public String getDescription() { return description; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public boolean isProcessed() { return isProcessed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentReport that = (CommentReport) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CommentReport{" +
                "id=" + id +
                ", commentId=" + commentId +
                ", reporterEmail='" + reporterEmail + '\'' +
                ", reason='" + reason + '\'' +
                ", description='" + description + '\'' +
                ", reportedAt=" + reportedAt +
                ", isProcessed=" + isProcessed +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long commentId;
        private String reporterEmail;
        private String reason;
        private String description;
        private LocalDateTime reportedAt = LocalDateTime.now();
        private boolean isProcessed = false;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder commentId(Long commentId) {
            this.commentId = commentId;
            return this;
        }

        public Builder reporterEmail(String reporterEmail) {
            this.reporterEmail = reporterEmail;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder reportedAt(LocalDateTime reportedAt) {
            this.reportedAt = reportedAt;
            return this;
        }

        public Builder isProcessed(boolean isProcessed) {
            this.isProcessed = isProcessed;
            return this;
        }

        public CommentReport build() {
            return new CommentReport(id, commentId, reporterEmail, reason, description, reportedAt, isProcessed);
        }
    }
}