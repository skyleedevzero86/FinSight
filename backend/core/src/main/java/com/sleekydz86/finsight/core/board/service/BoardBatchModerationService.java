package com.sleekydz86.finsight.core.board.service;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.port.in.BoardBatchModerationUseCase;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardBatchModerationService implements BoardBatchModerationUseCase {

    private static final Logger log = LoggerFactory.getLogger(BoardBatchModerationService.class);

    private final BoardPersistencePort boardPersistencePort;

    public BoardBatchModerationService(BoardPersistencePort boardPersistencePort) {
        this.boardPersistencePort = boardPersistencePort;
    }

    @Override
    @Transactional
    public int hideOverReportedActiveBoards(int reportThreshold) {
        if (reportThreshold < 1) {
            log.warn("과다 신고 게시글 숨김: 임계값 {}이(가) 유효하지 않아 1로 보정합니다", reportThreshold);
            reportThreshold = 1;
        }

        List<Board> targets = boardPersistencePort.findByStatusAndReportCountAtLeast(
                BoardStatus.ACTIVE, reportThreshold);

        int hidden = 0;
        for (Board board : targets) {
            if (!board.isActive()) {
                continue;
            }
            Board updated = board.updateStatus(BoardStatus.HIDDEN);
            boardPersistencePort.save(updated);
            hidden++;
        }

        if (hidden > 0) {
            log.info("과다 신고 게시글 숨김 실행 결과 - 임계값: {}, 숨김 처리 건수: {}", reportThreshold, hidden);
        }
        return hidden;
    }
}
