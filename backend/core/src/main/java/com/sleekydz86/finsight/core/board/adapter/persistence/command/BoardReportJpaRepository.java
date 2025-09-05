package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardReportJpaRepository extends JpaRepository<BoardReportJpaEntity, Long> {
    List<BoardReportJpaEntity> findByBoardIdOrderByReportedAtDesc(Long boardId);

    Optional<BoardReportJpaEntity> findByBoardIdAndReporterEmail(Long boardId, String reporterEmail);

    List<BoardReportJpaEntity> findByIsProcessedFalseOrderByReportedAtAsc();

    long countByBoardId(Long boardId);

    @Query("SELECT COUNT(br) FROM BoardReportJpaEntity br WHERE br.boardId = :boardId")
    long countReportsByBoard(@Param("boardId") Long boardId);
}