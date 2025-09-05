package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardReport;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReportPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoardReportRepositoryImpl implements BoardReportPersistencePort {

    private final BoardReportJpaRepository boardReportJpaRepository;
    private final BoardReportJpaMapper boardReportJpaMapper;

    public BoardReportRepositoryImpl(BoardReportJpaRepository boardReportJpaRepository,
                                     BoardReportJpaMapper boardReportJpaMapper) {
        this.boardReportJpaRepository = boardReportJpaRepository;
        this.boardReportJpaMapper = boardReportJpaMapper;
    }

    @Override
    public BoardReport save(BoardReport report) {
        BoardReportJpaEntity entity = boardReportJpaMapper.toEntity(report);
        BoardReportJpaEntity savedEntity = boardReportJpaRepository.save(entity);
        return boardReportJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<BoardReport> findById(Long reportId) {
        return boardReportJpaRepository.findById(reportId)
                .map(boardReportJpaMapper::toDomain);
    }

    @Override
    public List<BoardReport> findByBoardId(Long boardId) {
        List<BoardReportJpaEntity> entities = boardReportJpaRepository.findByBoardIdOrderByReportedAtDesc(boardId);
        return boardReportJpaMapper.toDomainList(entities);
    }

    @Override
    public Optional<BoardReport> findByBoardIdAndReporterEmail(Long boardId, String reporterEmail) {
        return boardReportJpaRepository.findByBoardIdAndReporterEmail(boardId, reporterEmail)
                .map(boardReportJpaMapper::toDomain);
    }

    @Override
    public List<BoardReport> findUnprocessedReports() {
        List<BoardReportJpaEntity> entities = boardReportJpaRepository.findByIsProcessedFalseOrderByReportedAtAsc();
        return boardReportJpaMapper.toDomainList(entities);
    }

    @Override
    public void deleteById(Long reportId) {
        boardReportJpaRepository.deleteById(reportId);
    }

    @Override
    public long countByBoardId(Long boardId) {
        return boardReportJpaRepository.countByBoardId(boardId);
    }
}