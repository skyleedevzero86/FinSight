package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardModerationRun;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardBatchModerationUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardModerationItemResponse;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.exception.BoardNotFoundException;
import com.sleekydz86.finsight.web.dto.BoardMaintenanceRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(
        name = "관리자 게시판",
        description = "신고 과다 게시글 미리보기·일괄 숨김·숨김 목록·복구/차단·실행 이력 API (ADMIN/MANAGER). MEDIA 타입 제외.")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/admin/boards/maintenance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminBoardMaintenanceController {

    private final BoardBatchModerationUseCase boardBatchModerationUseCase;

    public AdminBoardMaintenanceController(BoardBatchModerationUseCase boardBatchModerationUseCase) {
        this.boardBatchModerationUseCase = boardBatchModerationUseCase;
    }

    @Operation(
            summary = "신고 과다 후보 미리보기",
            description = "ACTIVE 커뮤니티 글 중 reportCount가 임계값 이상인 후보를 조회합니다. 실제 숨김은 하지 않습니다.")
    @GetMapping("/candidates")
    public ResponseEntity<ApiResponse<List<BoardModerationItemResponse>>> candidates(
            @Parameter(description = "신고 임계값 (기본 5)", example = "5")
            @RequestParam(defaultValue = "5") int reportThreshold) {
        log.info("신고 과다 후보 미리보기 - threshold={}", reportThreshold);
        List<BoardModerationItemResponse> items = boardBatchModerationUseCase
                .findHideCandidates(reportThreshold)
                .stream()
                .map(BoardModerationItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "신고 과다 후보를 조회했습니다"));
    }

    @Operation(
            summary = "신고 과다 게시글 숨김",
            description = "후보를 HIDDEN으로 일괄 변경하고 실행 이력을 저장합니다. 커뮤니티 타입(NOTICE/FREE/QNA/COMMUNITY)만 대상.")
    @PostMapping("/hide-over-reported")
    public ResponseEntity<ApiResponse<BoardMaintenanceRunResponse>> hideOverReported(
            @Parameter(description = "신고 임계값 (기본 5)", example = "5")
            @RequestParam(defaultValue = "5") int reportThreshold,
            @CurrentUser AuthenticatedUser currentUser) {
        log.info("신고 과다 숨김 수동 실행 - threshold={}, actor={}", reportThreshold, currentUser.getEmail());
        BoardModerationRun run = boardBatchModerationUseCase.hideOverReportedActiveBoards(
                reportThreshold, "MANUAL", currentUser.getEmail());
        return ResponseEntity.ok(
                ApiResponse.success(BoardMaintenanceRunResponse.from(run), "게시글 자동 숨김 처리가 완료되었습니다"));
    }

    @Operation(summary = "숨김 게시글 목록", description = "현재 HIDDEN 상태인 커뮤니티 게시글을 조회합니다.")
    @GetMapping("/hidden")
    public ResponseEntity<ApiResponse<List<BoardModerationItemResponse>>> hidden() {
        log.info("숨김 게시글 목록 조회");
        List<BoardModerationItemResponse> items = boardBatchModerationUseCase
                .findHiddenCommunityBoards()
                .stream()
                .map(BoardModerationItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "숨김 게시글을 조회했습니다"));
    }

    @Operation(summary = "게시글 복구", description = "HIDDEN/BLOCKED/REPORTED 게시글을 ACTIVE로 복구합니다.")
    @PostMapping("/boards/{boardId}/restore")
    public ResponseEntity<ApiResponse<BoardModerationItemResponse>> restore(@PathVariable Long boardId) {
        log.info("게시글 복구 - boardId={}", boardId);
        Board board = boardBatchModerationUseCase.restoreHiddenBoard(boardId);
        return ResponseEntity.ok(
                ApiResponse.success(BoardModerationItemResponse.from(board), "게시글을 복구했습니다"));
    }

    @Operation(summary = "게시글 영구 차단", description = "게시글을 BLOCKED로 변경합니다.")
    @PostMapping("/boards/{boardId}/block")
    public ResponseEntity<ApiResponse<BoardModerationItemResponse>> block(@PathVariable Long boardId) {
        log.info("게시글 영구 차단 - boardId={}", boardId);
        Board board = boardBatchModerationUseCase.blockBoard(boardId);
        return ResponseEntity.ok(
                ApiResponse.success(BoardModerationItemResponse.from(board), "게시글을 차단했습니다"));
    }

    @Operation(summary = "모더레이션 실행 이력", description = "최근 숨김 실행 이력을 조회합니다.")
    @GetMapping("/runs")
    public ResponseEntity<ApiResponse<List<BoardMaintenanceRunResponse>>> runs(
            @Parameter(description = "조회 건수 (1~100)", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        log.info("모더레이션 실행 이력 조회 - limit={}", limit);
        List<BoardMaintenanceRunResponse> items = boardBatchModerationUseCase.findRecentRuns(limit).stream()
                .map(BoardMaintenanceRunResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "모더레이션 실행 이력을 조회했습니다"));
    }

    @Operation(summary = "모더레이션 실행 상세", description = "특정 실행 이력의 숨김 대상 상세를 조회합니다.")
    @GetMapping("/runs/{runId}")
    public ResponseEntity<ApiResponse<BoardMaintenanceRunResponse>> runDetail(@PathVariable Long runId) {
        log.info("모더레이션 실행 상세 - runId={}", runId);
        BoardModerationRun run = boardBatchModerationUseCase
                .findRunById(runId)
                .orElseThrow(() -> new BoardNotFoundException(runId, "모더레이션 실행 이력을 찾을 수 없습니다"));
        return ResponseEntity.ok(
                ApiResponse.success(BoardMaintenanceRunResponse.from(run), "모더레이션 실행 상세를 조회했습니다"));
    }
}
