package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardModerationRun;
import com.sleekydz86.finsight.core.board.domain.BoardModerationTarget;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardBatchModerationUseCase;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardModerationRunPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.global.exception.BoardNotFoundException;
import com.sleekydz86.finsight.core.global.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class BoardBatchModerationService implements BoardBatchModerationUseCase {

    private static final Logger log = LoggerFactory.getLogger(BoardBatchModerationService.class);

    private static final List<BoardType> COMMUNITY_TYPES = List.of(
            BoardType.NOTICE,
            BoardType.COMMUNITY,
            BoardType.FREE,
            BoardType.QNA);

    private final BoardPersistencePort boardPersistencePort;
    private final BoardModerationRunPersistencePort moderationRunPersistencePort;

    public BoardBatchModerationService(
            BoardPersistencePort boardPersistencePort,
            BoardModerationRunPersistencePort moderationRunPersistencePort) {
        this.boardPersistencePort = boardPersistencePort;
        this.moderationRunPersistencePort = moderationRunPersistencePort;
    }

    @Override
    @Transactional
    public int hideOverReportedActiveBoards(int reportThreshold) {
        return hideOverReportedActiveBoards(reportThreshold, "BATCH", null).hiddenCount();
    }

    @Override
    @Transactional
    public BoardModerationRun hideOverReportedActiveBoards(
            int reportThreshold, String triggeredBy, String actorEmail) {
        int threshold = clampThreshold(reportThreshold);
        String trigger = normalizeTrigger(triggeredBy);

        List<Board> targets = boardPersistencePort.findModerationCandidates(
                BoardStatus.ACTIVE, threshold, COMMUNITY_TYPES);

        List<BoardModerationTarget> hiddenTargets = targets.stream()
                .filter(Board::isActive)
                .map(board -> {
                    Board updated = board.updateStatus(BoardStatus.HIDDEN);
                    boardPersistencePort.save(updated);
                    return toTarget(board);
                })
                .toList();

        BoardModerationRun run = new BoardModerationRun(
                null,
                trigger,
                blankToNull(actorEmail),
                threshold,
                hiddenTargets.size(),
                hiddenTargets,
                LocalDateTime.now());
        BoardModerationRun saved = moderationRunPersistencePort.save(run);

        log.info(
                "과다 신고 게시글 숨김 실행 - trigger={}, actor={}, threshold={}, hidden={}",
                trigger,
                actorEmail,
                threshold,
                saved.hiddenCount());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Board> findHideCandidates(int reportThreshold) {
        int threshold = clampThreshold(reportThreshold);
        return boardPersistencePort.findModerationCandidates(
                BoardStatus.ACTIVE, threshold, COMMUNITY_TYPES);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Board> findHiddenCommunityBoards() {
        return boardPersistencePort.findByStatusAndBoardTypes(BoardStatus.HIDDEN, COMMUNITY_TYPES);
    }

    @Override
    @Transactional
    public Board restoreHiddenBoard(Long boardId) {
        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException(boardId));
        assertCommunityType(board);
        if (board.getStatus() != BoardStatus.HIDDEN && board.getStatus() != BoardStatus.BLOCKED
                && board.getStatus() != BoardStatus.REPORTED) {
            throw new ValidationException(
                    "복구할 수 없는 상태입니다: " + board.getStatus(),
                    List.of("status"));
        }
        Board restored = board.updateStatus(BoardStatus.ACTIVE);
        Board saved = boardPersistencePort.save(restored);
        log.info("숨김/차단 게시글 복구 - boardId={}", boardId);
        return saved;
    }

    @Override
    @Transactional
    public Board blockBoard(Long boardId) {
        Board board = boardPersistencePort.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException(boardId));
        assertCommunityType(board);
        Board blocked = board.updateStatus(BoardStatus.BLOCKED);
        Board saved = boardPersistencePort.save(blocked);
        log.info("게시글 영구 차단 - boardId={}", boardId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardModerationRun> findRecentRuns(int limit) {
        return moderationRunPersistencePort.findRecent(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BoardModerationRun> findRunById(Long runId) {
        return moderationRunPersistencePort.findById(runId);
    }

    private void assertCommunityType(Board board) {
        if (board.getBoardType() == null || !COMMUNITY_TYPES.contains(board.getBoardType())) {
            throw new ValidationException(
                    "커뮤니티 게시글만 모더레이션할 수 있습니다",
                    List.of("boardType"));
        }
    }

    private static BoardModerationTarget toTarget(Board board) {
        return new BoardModerationTarget(
                board.getId(),
                board.getTitle(),
                board.getAuthorEmail(),
                board.getBoardType() != null ? board.getBoardType().name() : null,
                board.getReportCount());
    }

    private static int clampThreshold(int reportThreshold) {
        if (reportThreshold < 1) {
            log.warn("과다 신고 게시글 숨김: 임계값 {}이(가) 유효하지 않아 1로 보정합니다", reportThreshold);
            return 1;
        }
        return Math.min(reportThreshold, 1000);
    }

    private static String normalizeTrigger(String triggeredBy) {
        if (triggeredBy == null || triggeredBy.isBlank()) {
            return "MANUAL";
        }
        String value = triggeredBy.trim().toUpperCase(Locale.ROOT);
        if ("BATCH".equals(value) || "MANUAL".equals(value)) {
            return value;
        }
        return "MANUAL";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
