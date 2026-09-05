package com.sleekydz86.finsight.core.comment.domain.port.in.dto;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "댓글 모더레이션 대상 요약")
public record CommentModerationItemResponse(
        Long id,
        String title,
        String authorEmail,
        String boardType,
        String status,
        int reportCount,
        Long targetId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static CommentModerationItemResponse from(Comment comment) {
        String preview = comment.getContent() == null ? "" : comment.getContent().trim();
        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "…";
        }
        return new CommentModerationItemResponse(
                comment.getId(),
                preview,
                comment.getAuthorEmail(),
                comment.getCommentType() != null ? comment.getCommentType().name() : "COMMENT",
                comment.getStatus() != null ? comment.getStatus().name() : null,
                comment.getReportCount(),
                comment.getTargetId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}
