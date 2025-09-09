package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.port.BoardQueryUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardCommandUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.global.annotation.CurrentUser;
import com.sleekydz86.finsight.core.global.annotation.LogExecution;
import com.sleekydz86.finsight.core.global.annotation.PerformanceMonitor;
import com.sleekydz86.finsight.core.global.annotation.Retryable;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.AuthenticatedUser;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.SystemException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @Valid BoardSearchRequest request,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
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
            @PathVariable Long boardId,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
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
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            validateCreateRequest(request);
            Board board = boardCommandUseCase.createBoard(currentUser.getEmail(), request);
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
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            validateUpdateRequest(request);
            Board board = boardCommandUseCase.updateBoard(currentUser.getEmail(), boardId, request);
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
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            boardCommandUseCase.deleteBoard(currentUser.getEmail(), boardId);
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
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            Board board = boardCommandUseCase.likeBoard(currentUser.getEmail(), boardId);
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
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            Board board = boardCommandUseCase.dislikeBoard(currentUser.getEmail(), boardId);
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
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            validateReportRequest(request);
            boardCommandUseCase.reportBoard(currentUser.getEmail(), boardId, request);
            return ResponseEntity.ok(ApiResponse.success(null, "게시판이 성공적으로 신고되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 신고 처리 중 오류가 발생했습니다", "BOARD_REPORT_ERROR", e);
        }
    }

    @PostMapping("/{boardId}/scrap")
    @LogExecution("게시판 스크랩")
    @PerformanceMonitor(threshold = 1000, operation = "board_scrap")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Void>> scrapBoard(
            @PathVariable Long boardId,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            boardCommandUseCase.scrapBoard(currentUser.getEmail(), boardId);
            return ResponseEntity.ok(ApiResponse.success(null, "게시판이 성공적으로 스크랩되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 스크랩 처리 중 오류가 발생했습니다", "BOARD_SCRAP_ERROR", e);
        }
    }

    @DeleteMapping("/{boardId}/scrap")
    @LogExecution("게시판 스크랩 취소")
    @PerformanceMonitor(threshold = 1000, operation = "board_unscrap")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Void>> unscrapBoard(
            @PathVariable Long boardId,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }
            boardCommandUseCase.unscrapBoard(currentUser.getEmail(), boardId);
            return ResponseEntity.ok(ApiResponse.success(null, "게시판 스크랩이 성공적으로 취소되었습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 스크랩 취소 처리 중 오류가 발생했습니다", "BOARD_UNSCRAP_ERROR", e);
        }
    }

    @GetMapping("/my-scraps")
    @LogExecution("내 스크랩 목록 조회")
    @PerformanceMonitor(threshold = 2000, operation = "my_scraps")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getMyScraps(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (page < 0) {
                throw new ValidationException("페이지 번호는 0 이상이어야 합니다", Arrays.asList("page는 0 이상의 값이어야 합니다"));
            }
            if (size <= 0 || size > 100) {
                throw new ValidationException("페이지 크기는 1-100 사이여야 합니다", Arrays.asList("size는 1-100 사이의 값이어야 합니다"));
            }
            List<BoardListResponse> response = boardQueryUseCase.getMyScrappedBoards(currentUser.getEmail(), page, size);
            return ResponseEntity.ok(ApiResponse.success(response, "내 스크랩 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("내 스크랩 목록 조회 중 오류가 발생했습니다", "MY_SCRAPS_ERROR", e);
        }
    }

    @GetMapping("/my-boards")
    @LogExecution("내 게시판 목록 조회")
    @PerformanceMonitor(threshold = 2000, operation = "my_boards")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getMyBoards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (page < 0) {
                throw new ValidationException("페이지 번호는 0 이상이어야 합니다", Arrays.asList("page는 0 이상의 값이어야 합니다"));
            }
            if (size <= 0 || size > 100) {
                throw new ValidationException("페이지 크기는 1-100 사이여야 합니다", Arrays.asList("size는 1-100 사이의 값이어야 합니다"));
            }
            List<BoardListResponse> response = boardQueryUseCase.getMyBoards(currentUser.getEmail(), page, size);
            return ResponseEntity.ok(ApiResponse.success(response, "내 게시판 목록을 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("내 게시판 목록 조회 중 오류가 발생했습니다", "MY_BOARDS_ERROR", e);
        }
    }

    @GetMapping("/popular")
    @LogExecution("인기 게시판 조회")
    @PerformanceMonitor(threshold = 1000, operation = "board_popular")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getPopularBoards(
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
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
            @RequestParam(defaultValue = "10") int limit,
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
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
    public ResponseEntity<ApiResponse<BoardStatisticsResponse>> getBoardStatistics(
            @CurrentUser(required = false) AuthenticatedUser currentUser) {
        try {
            BoardStatisticsResponse response = boardQueryUseCase.getBoardStatistics();
            return ResponseEntity.ok(ApiResponse.success(response, "게시판 통계를 성공적으로 조회했습니다"));
        } catch (Exception e) {
            throw new SystemException("게시판 통계 조회 중 오류가 발생했습니다", "BOARD_STATISTICS_ERROR", e);
        }
    }

    @GetMapping("/{boardId}/reaction-status")
    @LogExecution("게시판 반응 상태 조회")
    @PerformanceMonitor(threshold = 1000, operation = "board_reaction_status")
    @Retryable(maxAttempts = 3, delay = 1000, retryFor = { Exception.class })
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getBoardReactionStatus(
            @PathVariable Long boardId,
            @CurrentUser AuthenticatedUser currentUser) {
        try {
            if (boardId == null || boardId <= 0) {
                throw new ValidationException("유효하지 않은 게시판 ID입니다", Arrays.asList("boardId는 1 이상의 양수여야 합니다"));
            }

            Map<String, Boolean> status = Map.of(
                    "liked", boardQueryUseCase.hasUserLikedBoard(currentUser.getEmail(), boardId),
                    "disliked", boardQueryUseCase.hasUserDislikedBoard(currentUser.getEmail(), boardId),
                    "scrapped", boardQueryUseCase.hasUserScrappedBoard(currentUser.getEmail(), boardId)
            );

            return ResponseEntity.ok(ApiResponse.success(status, "게시판 반응 상태를 성공적으로 조회했습니다"));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("게시판 반응 상태 조회 중 오류가 발생했습니다", "BOARD_REACTION_STATUS_ERROR", e);
        }
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