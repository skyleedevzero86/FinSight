package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardModerationTarget;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "모더레이션 대상/결과 게시글 요약")
public record BoardModerationItemResponse(
        Long id,
        String title,
        String authorEmail,
        String boardType,
        String status,
        int reportCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static BoardModerationItemResponse from(Board board) {
        return new BoardModerationItemResponse(
                board.getId(),
                board.getTitle(),
                board.getAuthorEmail(),
                board.getBoardType() != null ? board.getBoardType().name() : null,
                board.getStatus() != null ? board.getStatus().name() : null,
                board.getReportCount(),
                board.getCreatedAt(),
                board.getUpdatedAt());
    }

    public static BoardModerationItemResponse from(BoardModerationTarget target) {
        return new BoardModerationItemResponse(
                target.boardId(),
                target.title(),
                target.authorEmail(),
                target.boardType(),
                "HIDDEN",
                target.reportCount(),
                null,
                null);
    }
}
