package com.sleekydz86.finsight.core.board.domain;

import java.time.LocalDateTime;

public class BoardReport {
    private final Long id;
    private final Long boardId;
    private final String reporterEmail;
    private final String reason;
    private final String description;
    private final LocalDateTime reportedAt;
    private final boolean isProcessed;

    public BoardReport() {
        this.id = null;
        this.boardId = null;
        this.reporterEmail = "";
        this.reason = "";
        this.description = "";
        this.reportedAt = LocalDateTime.now();
        this.isProcessed = false;
    }

    public BoardReport(Long id, Long boardId, String reporterEmail, String reason,
                       String description, LocalDateTime reportedAt, boolean isProcessed) {
        this.id = id;
        this.boardId = boardId;
        this.reporterEmail = reporterEmail;
        this.reason = reason;
        this.description = description;
        this.reportedAt = reportedAt;
        this.isProcessed = isProcessed;
    }

    public BoardReport markAsProcessed() {
        return new BoardReport(this.id, this.boardId, this.reporterEmail, this.reason,
                this.description, this.reportedAt, true);
    }

    public Long getId() { return id; }
    public Long getBoardId() { return boardId; }
    public String getReporterEmail() { return reporterEmail; }
    public String getReason() { return reason; }
    public String getDescription() { return description; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public boolean isProcessed() { return isProcessed; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoardReport that = (BoardReport) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BoardReport{" +
                "id=" + id +
                ", boardId=" + boardId +
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
        private Long boardId;
        private String reporterEmail;
        private String reason;
        private String description;
        private LocalDateTime reportedAt = LocalDateTime.now();
        private boolean isProcessed = false;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder boardId(Long boardId) {
            this.boardId = boardId;
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

        public BoardReport build() {
            return new BoardReport(id, boardId, reporterEmail, reason, description, reportedAt, isProcessed);
        }
    }
}