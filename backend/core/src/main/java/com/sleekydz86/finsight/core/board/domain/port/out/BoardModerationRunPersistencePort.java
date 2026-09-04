package com.sleekydz86.finsight.core.board.domain.port.out;

import com.sleekydz86.finsight.core.board.domain.BoardModerationRun;

import java.util.List;
import java.util.Optional;

public interface BoardModerationRunPersistencePort {
    BoardModerationRun save(BoardModerationRun run);

    Optional<BoardModerationRun> findById(Long id);

    List<BoardModerationRun> findRecent(int limit);
}
