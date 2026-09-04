package com.sleekydz86.finsight.web.dto;

import com.sleekydz86.finsight.core.board.domain.BoardModerationRun;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardModerationItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "신고 과다 숨김 실행 결과/이력")
public record BoardMaintenanceRunResponse(
        Long runId,
        int hiddenCount,
        int reportThreshold,
        String triggeredBy,
        String actorEmail,
        LocalDateTime createdAt,
        List<BoardModerationItemResponse> items) {

    public static BoardMaintenanceRunResponse from(BoardModerationRun run) {
        List<BoardModerationItemResponse> items = run.targets() == null
                ? List.of()
                : run.targets().stream().map(BoardModerationItemResponse::from).toList();
        return new BoardMaintenanceRunResponse(
                run.id(),
                run.hiddenCount(),
                run.reportThreshold(),
                run.triggeredBy(),
                run.actorEmail(),
                run.createdAt(),
                items);
    }
}
