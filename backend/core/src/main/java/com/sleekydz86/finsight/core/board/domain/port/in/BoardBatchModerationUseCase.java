package com.sleekydz86.finsight.core.board.domain.port.in;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardModerationRun;

import java.util.List;
import java.util.Optional;

public interface BoardBatchModerationUseCase {

    int hideOverReportedActiveBoards(int reportThreshold);

    BoardModerationRun hideOverReportedActiveBoards(int reportThreshold, String triggeredBy, String actorEmail);

    List<Board> findHideCandidates(int reportThreshold);

    List<Board> findHiddenCommunityBoards();

    Board restoreHiddenBoard(Long boardId);

    Board blockBoard(Long boardId);

    List<BoardModerationRun> findRecentRuns(int limit);

    Optional<BoardModerationRun> findRunById(Long runId);
}
