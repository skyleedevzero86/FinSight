package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BoardReportRequest {
    @NotBlank(message = "신고 사유는 필수입니다")
    private final String reason;

    @Size(max = 500, message = "신고 설명은 500자를 초과할 수 없습니다")
    private final String description;

    public BoardReportRequest() {
        this.reason = "";
        this.description = "";
    }

    public BoardReportRequest(String reason, String description) {
        this.reason = reason;
        this.description = description;
    }

    public String getReason() { return reason; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "BoardReportRequest{" +
                "reason='" + reason + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}