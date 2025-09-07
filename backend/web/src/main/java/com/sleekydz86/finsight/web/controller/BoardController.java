package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.port.BoardQueryUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardCommandUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.*;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/boards")
public class BoardController {

    private final BoardQueryUseCase boardQueryUseCase;
    private final BoardCommandUseCase boardCommandUseCase;

    public BoardController(BoardQueryUseCase boardQueryUseCase, BoardCommandUseCase boardCommandUseCase) {
        this.boardQueryUseCase = boardQueryUseCase;
        this.boardCommandUseCase = boardCommandUseCase;
    }

    @GetMapping
    @LogExecution("게시판 목록 조회")
    @PerformanceMonitor(threshold = 2000, operation = "board_list")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<PaginationResponse<BoardListResponse>>> getBoards(
            @Valid BoardSearchRequest request) {
        try {
            validateSearchRequest(request);
            PaginationResponse<BoardListResponse> response = boardQueryUseCase.getBoards(request);
            return ResponseEntity.ok(ApiResponse.success(response, "게시판 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 목록 조회 중 오류가 발생했습니다", "BOARD_LIST_ERROR", e);
        }
    }

    @GetMapping("/{boardId}")
    @LogExecution("게시판 상세 조회")
    @PerformanceMonitor(threshold = 1000, operation = "board_detail")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoardDetail(
            @PathVariable Long boardId) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            BoardDetailResponse response = boardQueryUseCase.getBoardDetail(boardId);
            return ResponseEntity.ok(ApiResponse.success(response, "게시판 상세 정보를 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 상세 조회 중 오류가 발생했습니다", "BOARD_DETAIL_ERROR", e);
        }
    }

    @PostMapping
    @LogExecution("게시판 생성")
    @PerformanceMonitor(threshold = 3000, operation = "board_create")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Board>> createBoard(
            @RequestBody @Valid BoardCreateRequest request,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            validateCreateRequest(request);
            Board board = boardCommandUseCase.createBoard(userEmail, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(board, "게시판이 성공적으로 생성되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 생성 중 오류가 발생했습니다", "BOARD_CREATE_ERROR", e);
        }
    }

    @PutMapping("/{boardId}")
    @LogExecution("게시판 수정")
    @PerformanceMonitor(threshold = 2000, operation = "board_update")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Board>> updateBoard(
            @PathVariable Long boardId,
            @RequestBody @Valid BoardUpdateRequest request,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            validateUpdateRequest(request);
            Board board = boardCommandUseCase.updateBoard(userEmail, boardId, request);
            return ResponseEntity.ok(ApiResponse.success(board, "게시판이 성공적으로 수정되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 수정 중 오류가 발생했습니다", "BOARD_UPDATE_ERROR", e);
        }
    }

    @DeleteMapping("/{boardId}")
    @LogExecution("게시판 삭제")
    @PerformanceMonitor(threshold = 1000, operation = "board_delete")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable Long boardId,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            boardCommandUseCase.deleteBoard(userEmail, boardId);
            return ResponseEntity.ok(ApiResponse.success(null, "게시판이 성공적으로 삭제되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 삭제 중 오류가 발생했습니다", "BOARD_DELETE_ERROR", e);
        }
    }

    @PostMapping("/{boardId}/like")
    @LogExecution("게시판 좋아요")
    @PerformanceMonitor(threshold = 1000, operation = "board_like")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Board>> likeBoard(
            @PathVariable Long boardId,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            Board board = boardCommandUseCase.likeBoard(userEmail, boardId);
            return ResponseEntity.ok(ApiResponse.success(board, "좋아요가 성공적으로 처리되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 좋아요 처리 중 오류가 발생했습니다", "BOARD_LIKE_ERROR", e);
        }
    }

    @PostMapping("/{boardId}/dislike")
    @LogExecution("게시판 싫어요")
    @PerformanceMonitor(threshold = 1000, operation = "board_dislike")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Board>> dislikeBoard(
            @PathVariable Long boardId,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            Board board = boardCommandUseCase.dislikeBoard(userEmail, boardId);
            return ResponseEntity.ok(ApiResponse.success(board, "싫어요가 성공적으로 처리되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 싫어요 처리 중 오류가 발생했습니다", "BOARD_DISLIKE_ERROR", e);
        }
    }

    @PostMapping("/{boardId}/report")
    @LogExecution("게시판 신고")
    @PerformanceMonitor(threshold = 2000, operation = "board_report")
    @Retryable(maxAttempts = 2, delay = 2000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Void>> reportBoard(
            @PathVariable Long boardId,
            @RequestBody @Valid BoardReportRequest request,
            Authentication authentication) {
        try {
            String userEmail = getCurrentUserEmail(authentication);
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            validateReportRequest(request);
            boardCommandUseCase.reportBoard(userEmail, boardId, request);
            return ResponseEntity.ok(ApiResponse.success(null, "게시판이 성공적으로 신고되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 신고 처리 중 오류가 발생했습니다", "BOARD_REPORT_ERROR", e);
        }
    }

    @GetMapping("/popular")
    @LogExecution("인기 게시판 조회")
    @PerformanceMonitor(threshold = 1000, operation = "board_popular")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getPopularBoards(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit <= 0 || limit > 100) {
                throw new ValidationException("제한 수는 1-100 사이여야 합니다", Arrays.asList("limit은 1-100 사이의 값이어야 합니다"));
            }
            List<BoardListResponse> response = boardQueryUseCase.getPopularBoards(limit);
            return ResponseEntity.ok(ApiResponse.success(response, "인기 게시판을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("인기 게시판 조회 중 오류가 발생했습니다", "BOARD_POPULAR_ERROR", e);
        }
    }

    @GetMapping("/latest")
    @LogExecution("최신 게시판 조회")
    @PerformanceMonitor(threshold = 1000, operation = "board_latest")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getLatestBoards(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit <= 0 || limit > 100) {
                throw new ValidationException("제한 수는 1-100 사이여야 합니다", Arrays.asList("limit은 1-100 사이의 값이어야 합니다"));
            }
            List<BoardListResponse> response = boardQueryUseCase.getLatestBoards(limit);
            return ResponseEntity.ok(ApiResponse.success(response, "최신 게시판을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("최신 게시판 조회 중 오류가 발생했습니다", "BOARD_LATEST_ERROR", e);
        }
    }

    @GetMapping("/statistics")
    @LogExecution("게시판 통계 조회")
    @PerformanceMonitor(threshold = 2000, operation = "board_statistics")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<BoardStatisticsResponse>> getBoardStatistics() {
        try {
            BoardStatisticsResponse response = boardQueryUseCase.getBoardStatistics();
            return ResponseEntity.ok(ApiResponse.success(response, "게시판 통계를 성공적으로 조회했습니다"));
        } catch (Exception e) {
            throw new SystemException("게시판 통계 조회 중 오류가 발생했습니다", "BOARD_STATISTICS_ERROR", e);
        }
    }

    private String getCurrentUserEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("인증이 필요합니다", Arrays.asList("사용자 인증이 필요합니다"));
        }
        return authentication.getName();
    }

    private void validateSearchRequest(BoardSearchRequest request) {
        if (request == null) {
            throw new ValidationException("검색 요청이 null입니다", Arrays.asList("검색 요청은 null일 수 없습니다"));
        }
        if (request.getPage() < 0) {
            throw new ValidationException("페이지 번호는 0 이상이어야 합니다", Arrays.asList("page는 0 이상의 값이어야 합니다"));
        }
        if (request.getSize() <= 0 || request.getSize() > 100) {
            throw new ValidationException("페이지 크기는 1-100 사이여야 합니다", Arrays.asList("size는 1-100 사이의 값이어야 합니다"));
        }
    }

    private void validateCreateRequest(BoardCreateRequest request) {
        if (request == null) {
            throw new ValidationException("생성 요청이 null입니다", Arrays.asList("생성 요청은 null일 수 없습니다"));
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("제목은 필수입니다", Arrays.asList("title은 필수입니다"));
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("내용은 필수입니다", Arrays.asList("content는 필수입니다"));
        }
        if (request.getTitle().length() > 200) {
            throw new ValidationException("제목은 200자를 초과할 수 없습니다", Arrays.asList("title은 200자를 초과할 수 없습니다"));
        }
        if (request.getContent().length() > 10000) {
            throw new ValidationException("내용은 10000자를 초과할 수 없습니다", Arrays.asList("content는 10000자를 초과할 수 없습니다"));
        }
    }

    private void validateUpdateRequest(BoardUpdateRequest request) {
        if (request == null) {
            throw new ValidationException("수정 요청이 null입니다", Arrays.asList("수정 요청은 null일 수 없습니다"));
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("제목은 필수입니다", Arrays.asList("title은 필수입니다"));
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ValidationException("내용은 필수입니다", Arrays.asList("content는 필수입니다"));
        }
        if (request.getTitle().length() > 200) {
            throw new ValidationException("제목은 200자를 초과할 수 없습니다", Arrays.asList("title은 200자를 초과할 수 없습니다"));
        }
        if (request.getContent().length() > 10000) {
            throw new ValidationException("내용은 10000자를 초과할 수 없습니다", Arrays.asList("content는 10000자를 초과할 수 없습니다"));
        }
    }

    private void validateReportRequest(BoardReportRequest request) {
        if (request == null) {
            throw new ValidationException("신고 요청이 null입니다", Arrays.asList("신고 요청은 null일 수 없습니다"));
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new ValidationException("신고 사유는 필수입니다", Arrays.asList("reason은 필수입니다"));
        }
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new ValidationException("신고 설명은 500자를 초과할 수 없습니다", Arrays.asList("description은 500자를 초과할 수 없습니다"));
        }
    }
}