package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.port.in.BoardBatchModerationUseCase;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.web.dto.BoardMaintenanceRunResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/boards/maintenance")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBoardMaintenanceController {

    private final BoardBatchModerationUseCase boardBatchModerationUseCase;

    public AdminBoardMaintenanceController(BoardBatchModerationUseCase boardBatchModerationUseCase) {
        this.boardBatchModerationUseCase = boardBatchModerationUseCase;
    }

    @PostMapping("/hide-over-reported")
    public ResponseEntity<ApiResponse<BoardMaintenanceRunResponse>> hideOverReported(
            @RequestParam(defaultValue = "5") int reportThreshold) {
        int hidden = boardBatchModerationUseCase.hideOverReportedActiveBoards(reportThreshold);
        BoardMaintenanceRunResponse body = new BoardMaintenanceRunResponse(hidden, reportThreshold);
        return ResponseEntity.ok(ApiResponse.success(body, "게시글 자동 숨김 처리가 완료되었습니다"));
    }
}
