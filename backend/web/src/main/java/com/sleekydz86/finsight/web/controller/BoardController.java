package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardFile;
import com.sleekydz86.finsight.core.board.domain.BoardScrap;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardCommandUseCase;
import com.sleekydz86.finsight.core.board.domain.port.BoardQueryUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.global.dto.ApiResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardQueryUseCase boardQueryUseCase;
    private final BoardCommandUseCase boardCommandUseCase;

    public BoardController(BoardQueryUseCase boardQueryUseCase, BoardCommandUseCase boardCommandUseCase) {
        this.boardQueryUseCase = boardQueryUseCase;
        this.boardCommandUseCase = boardCommandUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginationResponse<BoardListResponse>>> getBoards(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String hashtag,
            @RequestParam(required = false) BoardType boardType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        BoardSearchRequest request = BoardSearchRequest.builder()
                .keyword(keyword)
                .hashtag(hashtag)
                .boardType(boardType)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PaginationResponse<BoardListResponse> response = boardQueryUseCase.getBoards(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoardDetail(@PathVariable Long boardId) {
        BoardDetailResponse response = boardQueryUseCase.getBoardDetail(boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{boardId}/navigation")
    public ResponseEntity<ApiResponse<BoardDetailResponse>> getBoardDetailWithNavigation(
            @PathVariable Long boardId, @RequestParam BoardType boardType) {
        BoardDetailResponse response = boardQueryUseCase.getBoardDetailWithNavigation(boardId, boardType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getPopularBoards(
            @RequestParam(defaultValue = "10") int limit) {
        List<BoardListResponse> response = boardQueryUseCase.getPopularBoards(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getLatestBoards(
            @RequestParam(defaultValue = "10") int limit) {
        List<BoardListResponse> response = boardQueryUseCase.getLatestBoards(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/category/{boardType}")
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getBoardsByCategory(
            @PathVariable BoardType boardType, @RequestParam(defaultValue = "10") int limit) {
        List<BoardListResponse> response = boardQueryUseCase.getBoardsByCategory(boardType, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-scraps")
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getMyScrappedBoards(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userEmail = authentication.getName();
        List<BoardListResponse> response = boardQueryUseCase.getMyScrappedBoards(userEmail, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-boards")
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getMyBoards(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userEmail = authentication.getName();
        List<BoardListResponse> response = boardQueryUseCase.getMyBoards(userEmail, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/reported")
    public ResponseEntity<ApiResponse<List<BoardListResponse>>> getReportedBoards() {
        List<BoardListResponse> response = boardQueryUseCase.getReportedBoards();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Board>> createBoard(
            Authentication authentication, @RequestBody BoardCreateRequest request) {
        String userEmail = authentication.getName();
        Board response = boardCommandUseCase.createBoard(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{boardId}")
    public ResponseEntity<ApiResponse<Board>> updateBoard(
            Authentication authentication, @PathVariable Long boardId, @RequestBody BoardUpdateRequest request) {
        String userEmail = authentication.getName();
        Board response = boardCommandUseCase.updateBoard(userEmail, boardId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        boardCommandUseCase.deleteBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{boardId}/like")
    public ResponseEntity<ApiResponse<Board>> likeBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        Board response = boardCommandUseCase.likeBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{boardId}/dislike")
    public ResponseEntity<ApiResponse<Board>> dislikeBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        Board response = boardCommandUseCase.dislikeBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{boardId}/report")
    public ResponseEntity<ApiResponse<Void>> reportBoard(
            Authentication authentication, @PathVariable Long boardId, @RequestBody BoardReportRequest request) {
        String userEmail = authentication.getName();
        boardCommandUseCase.reportBoard(userEmail, boardId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{boardId}/scrap")
    public ResponseEntity<ApiResponse<BoardScrap>> scrapBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        BoardScrap response = boardCommandUseCase.scrapBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{boardId}/scrap")
    public ResponseEntity<ApiResponse<Void>> unscrapBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        boardCommandUseCase.unscrapBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{boardId}/files")
    public ResponseEntity<ApiResponse<BoardFile>> uploadFile(
            Authentication authentication, @PathVariable Long boardId,
            @RequestParam String fileName, @RequestParam String filePath, @RequestParam long fileSize) {
        String userEmail = authentication.getName();
        BoardFile response = boardCommandUseCase.uploadFile(userEmail, boardId, fileName, filePath, fileSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            Authentication authentication, @PathVariable Long fileId) {
        String userEmail = authentication.getName();
        boardCommandUseCase.deleteFile(userEmail, fileId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<BoardStatisticsResponse>> getBoardStatistics() {
        BoardStatisticsResponse response = boardQueryUseCase.getBoardStatistics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/author-statistics")
    public ResponseEntity<ApiResponse<List<BoardAuthorStatisticsResponse>>> getAuthorStatistics() {
        List<BoardAuthorStatisticsResponse> response = boardQueryUseCase.getAuthorStatistics();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{boardId}/like-status")
    public ResponseEntity<ApiResponse<Boolean>> hasUserLikedBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        boolean response = boardQueryUseCase.hasUserLikedBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{boardId}/dislike-status")
    public ResponseEntity<ApiResponse<Boolean>> hasUserDislikedBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        boolean response = boardQueryUseCase.hasUserDislikedBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{boardId}/scrap-status")
    public ResponseEntity<ApiResponse<Boolean>> hasUserScrappedBoard(
            Authentication authentication, @PathVariable Long boardId) {
        String userEmail = authentication.getName();
        boolean response = boardQueryUseCase.hasUserScrappedBoard(userEmail, boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}