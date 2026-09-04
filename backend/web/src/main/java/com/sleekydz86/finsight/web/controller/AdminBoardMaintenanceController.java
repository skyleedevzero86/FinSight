package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.port.in.BoardBatchModerationUseCase;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.web.dto.BoardMaintenanceRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 게시판", description = "게시판 유지보수·신고 과다 숨김 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/admin/boards/maintenance")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBoardMaintenanceController {

    private final BoardBatchModerationUseCase boardBatchModerationUseCase;

    public AdminBoardMaintenanceController(BoardBatchModerationUseCase boardBatchModerationUseCase) {
        this.boardBatchModerationUseCase = boardBatchModerationUseCase;
    }

    @Operation(summary = "신고 과다 게시글 숨김", description = "신고 건수가 임계값을 넘는 활성 게시글을 일괄 숨김 처리합니다.")
    @PostMapping("/hide-over-reported")
    public ResponseEntity<ApiResponse<BoardMaintenanceRunResponse>> hideOverReported(
            @RequestParam(defaultValue = "5") int reportThreshold) {
        int hidden = boardBatchModerationUseCase.hideOverReportedActiveBoards(reportThreshold);
        BoardMaintenanceRunResponse body = new BoardMaintenanceRunResponse(hidden, reportThreshold);
        return ResponseEntity.ok(ApiResponse.success(body, "게시글 자동 숨김 처리가 완료되었습니다"));
    }
}
