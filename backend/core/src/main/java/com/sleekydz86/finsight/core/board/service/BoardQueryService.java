package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.port.BoardQueryUseCase;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardScrapPersistencePort;
import com.sleekydz86.finsight.core.board.domain.BoardScrap;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.BoardNotFoundException;
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

        public BoardQueryService(BoardPersistencePort boardPersistencePort,
                        BoardReactionPersistencePort boardReactionPersistencePort,
                        BoardScrapPersistencePort boardScrapPersistencePort) {
                this.boardPersistencePort = boardPersistencePort;
                this.boardReactionPersistencePort = boardReactionPersistencePort;
                this.boardScrapPersistencePort = boardScrapPersistencePort;
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
        public BoardDetailResponse getBoardDetail(Long boardId) {
                log.info("게시판 상세 조회 요청: boardId={}", boardId);

                Board board = boardPersistencePort.findById(boardId)
                                .orElseThrow(() -> new BoardNotFoundException(boardId));

                boardPersistencePort.incrementViewCount(boardId);

                return BoardDetailResponse.from(board);
        }

        @Override
        public BoardDetailResponse getBoardDetailWithNavigation(Long boardId, BoardType boardType) {
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

                boardPersistencePort.incrementViewCount(boardId);

                return BoardDetailResponse.from(board, navigation);
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
                                boardPersistencePort.countByBoardType(BoardType.COMMUNITY);

                Map<String, Long> boardsByType = Map.of(
                                "FREE", boardPersistencePort.countByBoardType(BoardType.FREE),
                                "NOTICE", boardPersistencePort.countByBoardType(BoardType.NOTICE),
                                "QNA", boardPersistencePort.countByBoardType(BoardType.QNA),
                                "COMMUNITY", boardPersistencePort.countByBoardType(BoardType.COMMUNITY));

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