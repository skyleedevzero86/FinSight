package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentBatchModerationUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentModerationItemResponse;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "관리자 댓글 모더레이션", description = "신고 과다 댓글 미리보기·일괄 숨김·숨김 목록·복구/차단 (ADMIN/MANAGER)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/admin/comments/maintenance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminCommentMaintenanceController {

    private final CommentBatchModerationUseCase commentBatchModerationUseCase;

    public AdminCommentMaintenanceController(CommentBatchModerationUseCase commentBatchModerationUseCase) {
        this.commentBatchModerationUseCase = commentBatchModerationUseCase;
    }

    @Operation(summary = "신고 과다 댓글 후보")
    @GetMapping("/candidates")
    public ResponseEntity<ApiResponse<List<CommentModerationItemResponse>>> candidates(
            @Parameter(description = "신고 임계값 (기본 5)", example = "5")
            @RequestParam(defaultValue = "5") int reportThreshold) {
        log.info("신고 과다 댓글 후보 미리보기 - threshold={}", reportThreshold);
        List<CommentModerationItemResponse> items = commentBatchModerationUseCase
                .findHideCandidates(reportThreshold)
                .stream()
                .map(CommentModerationItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "신고 과다 댓글 후보를 조회했습니다"));
    }

    @Operation(summary = "신고 과다 댓글 숨김")
    @PostMapping("/hide-over-reported")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hideOverReported(
            @Parameter(description = "신고 임계값 (기본 5)", example = "5")
            @RequestParam(defaultValue = "5") int reportThreshold,
            @CurrentUser AuthenticatedUser currentUser) {
        log.info("신고 과다 댓글 숨김 수동 실행 - threshold={}, actor={}", reportThreshold, currentUser.getEmail());
        int hiddenCount = commentBatchModerationUseCase.hideOverReportedActiveComments(
                reportThreshold, currentUser.getEmail());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runId", System.currentTimeMillis());
        body.put("hiddenCount", hiddenCount);
        body.put("reportThreshold", reportThreshold);
        body.put("triggeredBy", "MANUAL");
        body.put("actorEmail", currentUser.getEmail());
        body.put("items", List.of());
        return ResponseEntity.ok(ApiResponse.success(body, "댓글 자동 숨김 처리가 완료되었습니다"));
    }

    @Operation(summary = "숨김 댓글 목록")
    @GetMapping("/hidden")
    public ResponseEntity<ApiResponse<List<CommentModerationItemResponse>>> hidden() {
        log.info("숨김 댓글 목록 조회");
        List<CommentModerationItemResponse> items = commentBatchModerationUseCase
                .findHiddenComments()
                .stream()
                .map(CommentModerationItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(items, "숨김 댓글을 조회했습니다"));
    }

    @Operation(summary = "댓글 복구")
    @PostMapping("/comments/{commentId}/restore")
    public ResponseEntity<ApiResponse<CommentModerationItemResponse>> restore(@PathVariable Long commentId) {
        log.info("댓글 복구 - commentId={}", commentId);
        Comment comment = commentBatchModerationUseCase.restoreHiddenComment(commentId);
        return ResponseEntity.ok(
                ApiResponse.success(CommentModerationItemResponse.from(comment), "댓글을 복구했습니다"));
    }

    @Operation(summary = "댓글 영구 차단")
    @PostMapping("/comments/{commentId}/block")
    public ResponseEntity<ApiResponse<CommentModerationItemResponse>> block(@PathVariable Long commentId) {
        log.info("댓글 영구 차단 - commentId={}", commentId);
        Comment comment = commentBatchModerationUseCase.blockComment(commentId);
        return ResponseEntity.ok(
                ApiResponse.success(CommentModerationItemResponse.from(comment), "댓글을 차단했습니다"));
    }
}
