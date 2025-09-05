package com.sleekydz86.finsight.core.comment.domain.port.in.dto;

public class CommentReportRequest {
    private final String reason;
    private final String description;

    public CommentReportRequest() {
        this.reason = null;
        this.description = null;
    }

    public CommentReportRequest(String reason, String description) {
        this.reason = reason;
        this.description = description;
    }

    public String getReason() { return reason; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "CommentReportRequest{" +
                "reason='" + reason + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}