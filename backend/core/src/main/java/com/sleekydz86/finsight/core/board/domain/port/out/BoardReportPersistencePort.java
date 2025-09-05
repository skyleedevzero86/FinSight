package com.sleekydz86.finsight.core.board.domain.port.out;

import com.sleekydz86.finsight.core.board.domain.BoardReport;

import java.util.List;
import java.util.Optional;

public interface BoardReportPersistencePort {
    BoardReport save(BoardReport report);
    Optional<BoardReport> findById(Long reportId);
    List<BoardReport> findByBoardId(Long boardId);
    Optional<BoardReport> findByBoardIdAndReporterEmail(Long boardId, String reporterEmail);
    List<BoardReport> findUnprocessedReports();
    void deleteById(Long reportId);
    long countByBoardId(Long boardId);
}