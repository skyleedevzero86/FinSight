package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentCommandUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentQueryUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentReportRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentResponse;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentUpdateRequest;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.BaseException;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "댓글", description = "게시글 댓글 CRUD·반응·신고 API")
@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentQueryUseCase commentQueryUseCase;
    private final CommentCommandUseCase commentCommandUseCase;

    public CommentController(CommentQueryUseCase commentQueryUseCase, CommentCommandUseCase commentCommandUseCase) {
        this.commentQueryUseCase = commentQueryUseCase;
        this.commentCommandUseCase = commentCommandUseCase;
    }

    @Operation(summary = "게시판 댓글 목록 조회", description = "게시판 ID로 댓글 목록을 조회합니다.")
    @GetMapping("/board/{boardId}")
    @LogExecution("게시판 댓글 조회")
    @PerformanceMonitor(threshold = 2000, operation = "comment_list")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comments>> getCommentsByBoard(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", List.of("INVALID_BOARD_ID"));
            }
            if (page < 0) {
                throw new ValidationException("페이지 번호는 0 이상이어야 합니다", List.of("INVALID_PAGE"));
            }
            if (size <= 0 || size > 100) {
                throw new ValidationException("페이지 크기는 1-100 사이여야 합니다", List.of("INVALID_SIZE"));
            }

            Comments comments = commentQueryUseCase.getCommentsByTargetIdWithPagination(boardId, CommentType.BOARD, page, size);
            return ResponseEntity.ok(ApiResponse.success(comments, "댓글 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 목록 조회 중 오류가 발생했습니다", "COMMENT_LIST_ERROR", e);
        }
    }

    @Operation(summary = "댓글 상세 조회", description = "댓글 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{commentId}")
    @LogExecution("댓글 상세 조회")
    @PerformanceMonitor(threshold = 1000, operation = "comment_detail")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comment>> getCommentDetail(
            @PathVariable Long commentId,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        try {
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다", List.of("INVALID_COMMENT_ID"));
            }
            Comment comment = commentQueryUseCase.getCommentById(commentId);
            return ResponseEntity.ok(ApiResponse.success(comment, "댓글 상세 정보를 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 상세 조회 중 오류가 발생했습니다", "COMMENT_DETAIL_ERROR", e);
        }
    }

    @Operation(summary = "댓글 생성", description = "새 댓글을 생성합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @LogExecution("댓글 생성")
    @PerformanceMonitor(threshold = 2000, operation = "comment_create")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comment>> createComment(
            @RequestBody @Valid CommentCreateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            validateCreateRequest(request);
            Comment comment = commentCommandUseCase.createComment(
                    currentUser.getEmail(), currentUser.getRole(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(comment, "댓글이 성공적으로 생성되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 생성 중 오류가 발생했습니다", "COMMENT_CREATE_ERROR", e);
        }
    }

    @Operation(summary = "댓글 수정", description = "기존 댓글을 수정합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{commentId}")
    @LogExecution("댓글 수정")
    @PerformanceMonitor(threshold = 2000, operation = "comment_update")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comment>> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다", List.of("INVALID_COMMENT_ID"));
            }
            validateUpdateRequest(request);
            Comment comment = commentCommandUseCase.updateComment(currentUser.getEmail(), commentId, request);
            return ResponseEntity.ok(ApiResponse.success(comment, "댓글이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 수정 중 오류가 발생했습니다", "COMMENT_UPDATE_ERROR", e);
        }
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{commentId}")
    @LogExecution("댓글 삭제")
    @PerformanceMonitor(threshold = 1000, operation = "comment_delete")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다", List.of("INVALID_COMMENT_ID"));
            }
            commentCommandUseCase.deleteComment(currentUser.getEmail(), currentUser.getRole(), commentId);
            return ResponseEntity.ok(ApiResponse.success(null, "댓글이 성공적으로 삭제되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 삭제 중 오류가 발생했습니다", "COMMENT_DELETE_ERROR", e);
        }
    }

    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 처리합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{commentId}/like")
    @LogExecution("댓글 좋아요")
    @PerformanceMonitor(threshold = 1000, operation = "comment_like")
    public ResponseEntity<ApiResponse<Comment>> likeComment(
            @PathVariable Long commentId,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다", List.of("INVALID_COMMENT_ID"));
            }
            Comment comment = commentCommandUseCase.likeComment(currentUser.getEmail(), commentId);
            return ResponseEntity.ok(ApiResponse.success(comment, "댓글 좋아요가 성공적으로 처리되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 좋아요 처리 중 오류가 발생했습니다", "COMMENT_LIKE_ERROR", e);
        }
    }

    @Operation(summary = "댓글 싫어요", description = "댓글에 싫어요를 처리합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{commentId}/dislike")
    @LogExecution("댓글 싫어요")
    @PerformanceMonitor(threshold = 1000, operation = "comment_dislike")
    public ResponseEntity<ApiResponse<Comment>> dislikeComment(
            @PathVariable Long commentId,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다", List.of("INVALID_COMMENT_ID"));
            }
            Comment comment = commentCommandUseCase.dislikeComment(currentUser.getEmail(), commentId);
            return ResponseEntity.ok(ApiResponse.success(comment, "댓글 싫어요가 성공적으로 처리되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 싫어요 처리 중 오류가 발생했습니다", "COMMENT_DISLIKE_ERROR", e);
        }
    }

    @Operation(summary = "댓글 신고", description = "댓글을 신고합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{commentId}/report")
    @LogExecution("댓글 신고")
    @PerformanceMonitor(threshold = 2000, operation = "comment_report")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentReportRequest request,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다", List.of("INVALID_COMMENT_ID"));
            }
            validateReportRequest(request);
            commentCommandUseCase.reportComment(currentUser.getEmail(), commentId, request);
            return ResponseEntity.ok(ApiResponse.success(null, "댓글이 성공적으로 신고되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 신고 처리 중 오류가 발생했습니다", "COMMENT_REPORT_ERROR", e);
        }
    }

    @Operation(summary = "내 댓글 목록 조회", description = "로그인한 사용자가 작성한 댓글 목록을 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my-comments")
    @LogExecution("내 댓글 목록 조회")
    @PerformanceMonitor(threshold = 2000, operation = "my_comments")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<PaginationResponse<java.util.Map<String, Object>>>> getMyComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (page < 0) {
                throw new ValidationException("페이지 번호는 0 이상이어야 합니다", List.of("INVALID_PAGE"));
            }
            if (size <= 0 || size > 100) {
                throw new ValidationException("페이지 크기는 1-100 사이여야 합니다", List.of("INVALID_SIZE"));
            }

            PaginationResponse<java.util.Map<String, Object>> comments =
                    commentQueryUseCase.getCommentsByUserEmail(currentUser.getEmail(), page, size);
            return ResponseEntity.ok(ApiResponse.success(comments, "내 댓글 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("내 댓글 목록 조회 중 오류가 발생했습니다", "MY_COMMENTS_ERROR", e);
        }
    }

    @Operation(summary = "내 댓글 반응 목록", description = "로그인한 사용자가 댓글에 남긴 좋아요·싫어요 기록을 조회합니다.")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my-reactions")
    @LogExecution("내 댓글 반응 목록 조회")
    @PerformanceMonitor(threshold = 2000, operation = "my_comment_reactions")
    public ResponseEntity<ApiResponse<PaginationResponse<java.util.Map<String, Object>>>> getMyReactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (page < 0) {
                throw new ValidationException("페이지 번호는 0 이상이어야 합니다", List.of("INVALID_PAGE"));
            }
            if (size <= 0 || size > 100) {
                throw new ValidationException("페이지 크기는 1-100 사이여야 합니다", List.of("INVALID_SIZE"));
            }
            PaginationResponse<java.util.Map<String, Object>> reactions =
                    commentQueryUseCase.getMyReactions(currentUser.getEmail(), page, size);
            return ResponseEntity.ok(ApiResponse.success(reactions, "내 댓글 반응 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("내 댓글 반응 목록 조회 중 오류가 발생했습니다", "MY_COMMENT_REACTIONS_ERROR", e);
        }
    }

    private void validateCreateRequest(CommentCreateRequest request) {
        if (request == null) {
            throw new ValidationException("댓글 생성 요청이 null입니다", List.of("REQUEST_NULL"));
        }
        if (request.getTargetId() == null || request.getTargetId() <= 0) {
            throw new ValidationException("유효하지 않은 게시판 ID입니다", List.of("INVALID_TARGET_ID"));
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("댓글 내용은 필수입니다", List.of("CONTENT_REQUIRED"));
        }
        if (request.getContent().length() > 1000) {
            throw new ValidationException("댓글 내용은 1000자를 초과할 수 없습니다", List.of("CONTENT_TOO_LONG"));
        }
    }

    private void validateUpdateRequest(CommentUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("댓글 수정 요청이 null입니다", List.of("REQUEST_NULL"));
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("댓글 내용은 필수입니다", List.of("CONTENT_REQUIRED"));
        }
        if (request.getContent().length() > 1000) {
            throw new ValidationException("댓글 내용은 1000자를 초과할 수 없습니다", List.of("CONTENT_TOO_LONG"));
        }
    }

    private void validateReportRequest(CommentReportRequest request) {
        if (request == null) {
            throw new ValidationException("댓글 신고 요청이 null입니다", List.of("REQUEST_NULL"));
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new ValidationException("신고 사유는 필수입니다", List.of("REASON_REQUIRED"));
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new ValidationException("신고 설명은 500자를 초과할 수 없습니다", List.of("DESCRIPTION_TOO_LONG"));
        }
    }
}
