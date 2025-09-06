package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentCommandUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.CommentQueryUseCase;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentReportRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentResponse;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentUpdateRequest;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {

    private final CommentQueryUseCase commentQueryUseCase;
    private final CommentCommandUseCase commentCommandUseCase;

    public CommentController(CommentQueryUseCase commentQueryUseCase, CommentCommandUseCase commentCommandUseCase) {
        this.commentQueryUseCase = commentQueryUseCase;
        this.commentCommandUseCase = commentCommandUseCase;
    }

    @GetMapping("/board/{boardId}")
    @LogExecution("게시판 댓글 조회")
    @PerformanceMonitor(threshold = 2000, operation = "comment_list")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<PaginationResponse<CommentResponse>>> getCommentsByBoard(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다");
            }
            if (page < 0) {
                throw new ValidationException("페이지 번호는 0 이상이어야 합니다");
            }
            if (size <= 0 || size > 100) {
                throw new ValidationException("페이지 크기는 1-100 사이여야 합니다");
            }

            PaginationResponse<CommentResponse> response = commentQueryUseCase.getCommentsByBoard(boardId, page, size);
            return ResponseEntity.ok(ApiResponse.success(response, "댓글 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 목록 조회 중 오류가 발생했습니다", "COMMENT_LIST_ERROR", e);
        }
    }

    @GetMapping("/{commentId}")
    @LogExecution("댓글 상세 조회")
    @PerformanceMonitor(threshold = 1000, operation = "comment_detail")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentDetail(@PathVariable Long commentId) {
        try {
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다");
            }
            CommentResponse response = commentQueryUseCase.getCommentById(commentId);
            return ResponseEntity.ok(ApiResponse.success(response, "댓글 상세 정보를 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 상세 조회 중 오류가 발생했습니다", "COMMENT_DETAIL_ERROR", e);
        }
    }

    @PostMapping
    @LogExecution("댓글 생성")
    @PerformanceMonitor(threshold = 2000, operation = "comment_create")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comment>> createComment(
            @RequestBody @Valid CommentCreateRequest request,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            validateCreateRequest(request);
            Comment comment = commentCommandUseCase.createComment(userEmail, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(comment, "댓글이 성공적으로 생성되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 생성 중 오류가 발생했습니다", "COMMENT_CREATE_ERROR", e);
        }
    }

    @PutMapping("/{commentId}")
    @LogExecution("댓글 수정")
    @PerformanceMonitor(threshold = 2000, operation = "comment_update")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comment>> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다");
            }
            validateUpdateRequest(request);
            Comment comment = commentCommandUseCase.updateComment(userEmail, commentId, request);
            return ResponseEntity.ok(ApiResponse.success(comment, "댓글이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 수정 중 오류가 발생했습니다", "COMMENT_UPDATE_ERROR", e);
        }
    }

    @DeleteMapping("/{commentId}")
    @LogExecution("댓글 삭제")
    @PerformanceMonitor(threshold = 1000, operation = "comment_delete")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다");
            }
            commentCommandUseCase.deleteComment(userEmail, commentId);
            return ResponseEntity.ok(ApiResponse.success(null, "댓글이 성공적으로 삭제되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 삭제 중 오류가 발생했습니다", "COMMENT_DELETE_ERROR", e);
        }
    }

    @PostMapping("/{commentId}/like")
    @LogExecution("댓글 좋아요")
    @PerformanceMonitor(threshold = 1000, operation = "comment_like")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comment>> likeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다");
            }
            Comment comment = commentCommandUseCase.likeComment(userEmail, commentId);
            return ResponseEntity.ok(ApiResponse.success(comment, "댓글 좋아요가 성공적으로 처리되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 좋아요 처리 중 오류가 발생했습니다", "COMMENT_LIKE_ERROR", e);
        }
    }

    @PostMapping("/{commentId}/dislike")
    @LogExecution("댓글 싫어요")
    @PerformanceMonitor(threshold = 1000, operation = "comment_dislike")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Comment>> dislikeComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다");
            }
            Comment comment = commentCommandUseCase.dislikeComment(userEmail, commentId);
            return ResponseEntity.ok(ApiResponse.success(comment, "댓글 싫어요가 성공적으로 처리되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 싫어요 처리 중 오류가 발생했습니다", "COMMENT_DISLIKE_ERROR", e);
        }
    }

    @PostMapping("/{commentId}/report")
    @LogExecution("댓글 신고")
    @PerformanceMonitor(threshold = 2000, operation = "comment_report")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = {Exception.class})
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentReportRequest request,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (commentId == null || commentId <= 0) {
                throw new ValidationException("유효하지 않은 댓글 ID입니다");
            }
            validateReportRequest(request);
            commentCommandUseCase.reportComment(userEmail, commentId, request);
            return ResponseEntity.ok(ApiResponse.success(null, "댓글이 성공적으로 신고되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("댓글 신고 처리 중 오류가 발생했습니다", "COMMENT_REPORT_ERROR", e);
        }
    }

    private String getCurrentUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("인증이 필요합니다");
        }
        return authentication.getName();
    }

    private void validateCreateRequest(CommentCreateRequest request) {
        if (request == null) {
            throw new ValidationException("댓글 생성 요청이 null입니다");
        }
        if (request.getBoardId() == null || request.getBoardId() <= 0) {
            throw new ValidationException("유효하지 않은 게시판 ID입니다");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("댓글 내용은 필수입니다");
        }
        if (request.getContent().length() > 1000) {
            throw new ValidationException("댓글 내용은 1000자를 초과할 수 없습니다");
        }
    }

    private void validateUpdateRequest(CommentUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("댓글 수정 요청이 null입니다");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("댓글 내용은 필수입니다");
        }
        if (request.getContent().length() > 1000) {
            throw new ValidationException("댓글 내용은 1000자를 초과할 수 없습니다");
        }
    }

    private void validateReportRequest(CommentReportRequest request) {
        if (request == null) {
            throw new ValidationException("댓글 신고 요청이 null입니다");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new ValidationException("신고 사유는 필수입니다");
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new ValidationException("신고 설명은 500자를 초과할 수 없습니다");
        }
    }
}