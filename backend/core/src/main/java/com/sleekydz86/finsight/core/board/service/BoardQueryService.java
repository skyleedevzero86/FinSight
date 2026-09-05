package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.port.BoardQueryUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.board.markdown.MarkdownRenderingService;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardScrapPersistencePort;
import com.sleekydz86.finsight.core.board.domain.BoardScrap;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.BoardNotFoundException;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BoardQueryService implements BoardQueryUseCase {

        private static final Logger log = LoggerFactory.getLogger(BoardQueryService.class);

        private final BoardPersistencePort boardPersistencePort;
        private final BoardReactionPersistencePort boardReactionPersistencePort;
        private final BoardScrapPersistencePort boardScrapPersistencePort;
        private final MarkdownRenderingService markdownRenderingService;
        private final BoardViewCountService boardViewCountService;

        public BoardQueryService(BoardPersistencePort boardPersistencePort,
                        BoardReactionPersistencePort boardReactionPersistencePort,
                        BoardScrapPersistencePort boardScrapPersistencePort,
                        MarkdownRenderingService markdownRenderingService,
                        BoardViewCountService boardViewCountService) {
                this.boardPersistencePort = boardPersistencePort;
                this.boardReactionPersistencePort = boardReactionPersistencePort;
                this.boardScrapPersistencePort = boardScrapPersistencePort;
                this.markdownRenderingService = markdownRenderingService;
                this.boardViewCountService = boardViewCountService;
        }

        @Override
        public PaginationResponse<BoardListResponse> getBoards(BoardSearchRequest request) {
                log.info("게시판 목록 조회 요청: {}", request);

                var boards = boardPersistencePort.findBySearchRequest(request);
                List<BoardListResponse> responses = boards.getBoards().stream()
                                .map(BoardListResponse::from)
                                .collect(Collectors.toList());

                return PaginationResponse.<BoardListResponse>builder()
                                .content(responses)
                                .page(request.getPage())
                                .size(request.getSize())
                                .totalElements(boards.getTotalElements())
                                .build();
        }

        @Override
        public PaginationResponse<BoardListResponse> getEditorDocuments(
                        BoardType boardType, BoardStatus status, String keyword, int page, int size) {
                var boards = boardPersistencePort.findEditorDocuments(boardType, status, keyword, page, size);
                List<BoardListResponse> responses = boards.getBoards().stream()
                                .map(BoardListResponse::from)
                                .collect(Collectors.toList());
                return PaginationResponse.<BoardListResponse>builder()
                                .content(responses)
                                .page(page)
                                .size(size)
                                .totalElements(boards.getTotalElements())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public BoardDetailResponse getBoardDetail(Long boardId) {
                return getBoardDetail(boardId, null, false, true);
        }

        @Override
        @Transactional(readOnly = true)
        public BoardDetailResponse getBoardDetail(Long boardId, String viewerEmail, boolean staffViewer) {
                return getBoardDetail(boardId, viewerEmail, staffViewer, true);
        }

        @Override
        @Transactional(readOnly = true)
        public BoardDetailResponse getBoardDetail(Long boardId, String viewerEmail, boolean staffViewer, boolean incrementViewCount) {
                log.info("게시판 상세 조회 요청: boardId={}", boardId);

                Board board = boardPersistencePort.findById(boardId)
                                .orElseThrow(() -> new BoardNotFoundException(boardId));

                assertReadable(board, viewerEmail, staffViewer);

                if (incrementViewCount) {
                        boardViewCountService.incrementSafely(boardId);
                }

                return BoardDetailResponse.from(board, markdownRenderingService.render(board.getContent()));
        }

        private void assertReadable(Board board, String viewerEmail, boolean staffViewer) {
                if (board.getStatus() == BoardStatus.ACTIVE) {
                        return;
                }
                if (board.getStatus() == BoardStatus.PRIVATE) {
                        if (staffViewer) {
                                return;
                        }
                        if (viewerEmail != null && viewerEmail.equalsIgnoreCase(board.getAuthorEmail())) {
                                return;
                        }
                        throw new InsufficientPermissionException("PRIVATE_BOARD", "비공개 글은 작성자와 관리자만 볼 수 있습니다");
                }
                throw new BoardNotFoundException(board.getId());
        }

        @Override
        @Transactional(readOnly = true)
        public BoardDetailResponse getBoardDetailWithNavigation(Long boardId, BoardType boardType) {
                return getBoardDetailWithNavigation(boardId, boardType, true);
        }

        @Override
        @Transactional(readOnly = true)
        public BoardDetailResponse getBoardDetailWithNavigation(Long boardId, BoardType boardType, boolean incrementViewCount) {
                log.info("게시판 상세 조회 (네비게이션 포함) 요청: boardId={}, boardType={}", boardId, boardType);

                Board board = boardPersistencePort.findById(boardId)
                                .orElseThrow(() -> new BoardNotFoundException(boardId));

                List<Board> navigationBoards = boardPersistencePort.findPreviousAndNext(boardId, boardType);

                BoardNavigationResponse navigation = null;
                if (navigationBoards.size() >= 2) {
                        Board previous = navigationBoards.get(0);
                        Board next = navigationBoards.get(1);
                        navigation = new BoardNavigationResponse(
                                        new BoardNavigationResponse.BoardNavigationItem(
                                                        previous.getId(),
                                                        previous.getTitle(),
                                                        previous.getAuthorEmail(),
                                                        previous.getCreatedAt().toString()),
                                        new BoardNavigationResponse.BoardNavigationItem(
                                                        next.getId(),
                                                        next.getTitle(),
                                                        next.getAuthorEmail(),
                                                        next.getCreatedAt().toString()));
                }

                if (incrementViewCount) {
                        boardViewCountService.incrementSafely(boardId);
                }

                return BoardDetailResponse.from(board, navigation, markdownRenderingService.render(board.getContent()));
        }

        @Override
        public List<BoardListResponse> getPopularBoards(int limit) {
                log.info("인기 게시판 조회 요청: limit={}", limit);

                var boards = boardPersistencePort.findPopularBoards(limit);
                return boards.getBoards().stream()
                                .map(BoardListResponse::from)
                                .collect(Collectors.toList());
        }

        @Override
        public List<BoardListResponse> getLatestBoards(int limit) {
                log.info("최신 게시판 조회 요청: limit={}", limit);

                var boards = boardPersistencePort.findLatestBoards(limit);
                return boards.getBoards().stream()
                                .map(BoardListResponse::from)
                                .collect(Collectors.toList());
        }

        @Override
        public List<BoardListResponse> getBoardsByCategory(BoardType boardType, int limit) {
                log.info("카테고리별 게시판 조회 요청: boardType={}, limit={}", boardType, limit);

                var boards = boardPersistencePort.findByBoardType(boardType, 0, limit);
                return boards.getBoards().stream()
                                .map(BoardListResponse::from)
                                .collect(Collectors.toList());
        }

        @Override
        public List<BoardListResponse> getMyScrappedBoards(String userEmail, int page, int size) {
                log.info("사용자 스크랩 게시판 조회 요청: user={}, page={}, size={}", userEmail, page, size);

                List<BoardScrap> scraps = boardScrapPersistencePort.findByUserEmail(userEmail, page, size);
                return scraps.stream()
                                .map(scrap -> {
                                        Board board = boardPersistencePort.findById(scrap.getBoardId())
                                                        .orElseThrow(() -> new BoardNotFoundException(
                                                                        scrap.getBoardId()));
                                        return BoardListResponse.from(board);
                                })
                                .collect(Collectors.toList());
        }

        @Override
        public List<BoardListResponse> getMyBoards(String userEmail, int page, int size) {
                log.info("사용자 작성 게시판 조회 요청: user={}, page={}, size={}", userEmail, page, size);

                var boards = boardPersistencePort.findByAuthorEmail(userEmail, page, size);
                return boards.getBoards().stream()
                                .map(BoardListResponse::from)
                                .collect(Collectors.toList());
        }

        @Override
        public List<Map<String, Object>> getMyReactions(String userEmail, int page, int size) {
                return boardReactionPersistencePort.findByUserEmail(userEmail, page, size).stream()
                                .map(reaction -> {
                                        Map<String, Object> row = new java.util.LinkedHashMap<>();
                                        row.put("boardId", reaction.getBoardId());
                                        row.put("reactionType", reaction.getReactionType() != null
                                                        ? reaction.getReactionType().name()
                                                        : null);
                                        row.put("createdAt", reaction.getCreatedAt());
                                        boardPersistencePort.findById(reaction.getBoardId()).ifPresentOrElse(board -> {
                                                row.put("title", board.getTitle());
                                                row.put("boardType", board.getBoardType() != null
                                                                ? board.getBoardType().name()
                                                                : null);
                                        }, () -> {
                                                row.put("title", "(삭제된 글)");
                                                row.put("boardType", null);
                                        });
                                        return row;
                                })
                                .collect(Collectors.toList());
        }

        @Override
        public List<BoardListResponse> getReportedBoards() {
                log.info("신고된 게시판 조회 요청");

                var boards = boardPersistencePort.findReportedBoards();
                return boards.getBoards().stream()
                                .map(BoardListResponse::from)
                                .collect(Collectors.toList());
        }

        @Override
        public boolean hasUserLikedBoard(String userEmail, Long boardId) {
                return boardReactionPersistencePort.findByBoardIdAndUserEmail(boardId, userEmail)
                                .map(reaction -> reaction.getReactionType() == ReactionType.LIKE)
                                .orElse(false);
        }

        @Override
        public boolean hasUserDislikedBoard(String userEmail, Long boardId) {
                return boardReactionPersistencePort.findByBoardIdAndUserEmail(boardId, userEmail)
                                .map(reaction -> reaction.getReactionType() == ReactionType.DISLIKE)
                                .orElse(false);
        }

        @Override
        public boolean hasUserScrappedBoard(String userEmail, Long boardId) {
                return boardScrapPersistencePort.findByBoardIdAndUserEmail(boardId, userEmail).isPresent();
        }

        @Override
        public BoardStatisticsResponse getBoardStatistics() {
                log.info("게시판 통계 조회 요청");

                long totalBoards = boardPersistencePort.countByBoardType(BoardType.FREE) +
                                boardPersistencePort.countByBoardType(BoardType.NOTICE) +
                                boardPersistencePort.countByBoardType(BoardType.QNA) +
                                boardPersistencePort.countByBoardType(BoardType.COMMUNITY) +
                                boardPersistencePort.countByBoardType(BoardType.MEDIA);

                Map<String, Long> boardsByType = Map.of(
                                "FREE", boardPersistencePort.countByBoardType(BoardType.FREE),
                                "NOTICE", boardPersistencePort.countByBoardType(BoardType.NOTICE),
                                "QNA", boardPersistencePort.countByBoardType(BoardType.QNA),
                                "COMMUNITY", boardPersistencePort.countByBoardType(BoardType.COMMUNITY),
                                "MEDIA", boardPersistencePort.countByBoardType(BoardType.MEDIA));

                Map<String, Long> boardsByStatus = Map.of(
                                "ACTIVE", totalBoards,
                                "BLOCKED", 0L,
                                "DELETED", 0L);

                Map<String, Long> dailyBoardCount = Map.of(
                                LocalDateTime.now().toLocalDate().toString(), totalBoards);

                Map<String, Long> weeklyBoardCount = Map.of(
                                "WEEK_" + LocalDateTime.now().getDayOfYear() / 7, totalBoards);

                Map<String, Long> monthlyBoardCount = Map.of(
                                LocalDateTime.now().getYear() + "-"
                                                + String.format("%02d", LocalDateTime.now().getMonthValue()),
                                totalBoards);

                return new BoardStatisticsResponse(
                                totalBoards,
                                totalBoards * 10L,
                                totalBoards * 5L,
                                totalBoards * 3L,
                                0L,
                                boardsByType,
                                boardsByStatus,
                                dailyBoardCount,
                                weeklyBoardCount,
                                monthlyBoardCount,
                                LocalDateTime.now());
        }

        @Override
        public List<BoardAuthorStatisticsResponse> getAuthorStatistics() {
                log.info("작성자 통계 조회 요청");

                return List.of(
                                new BoardAuthorStatisticsResponse(
                                                "admin@finsight.com",
                                                10L,
                                                1000L,
                                                500L,
                                                300L,
                                                100.0,
                                                50.0),
                                new BoardAuthorStatisticsResponse(
                                                "user@finsight.com",
                                                5L,
                                                500L,
                                                250L,
                                                150L,
                                                100.0,
                                                50.0));
        }
}
