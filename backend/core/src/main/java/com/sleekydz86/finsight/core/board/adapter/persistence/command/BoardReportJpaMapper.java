package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardReport;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BoardReportJpaMapper {

    public BoardReport toDomain(BoardReportJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BoardReport.builder()
                .id(entity.getId())
                .boardId(entity.getBoardId())
                .reporterEmail(entity.getReporterEmail())
                .reason(entity.getReason())
                .description(entity.getDescription())
                .reportedAt(entity.getReportedAt())
                .isProcessed(entity.isProcessed())
                .build();
    }

    public BoardReportJpaEntity toEntity(BoardReport report) {
        if (report == null) {
            return null;
        }

        return new BoardReportJpaEntity(
                report.getId(),
                report.getBoardId(),
                report.getReporterEmail(),
                report.getReason(),
                report.getDescription(),
                report.getReportedAt(),
                report.isProcessed(),
                report.getReportedAt(),
                report.getReportedAt()
        );
    }

    public List<BoardReport> toDomainList(List<BoardReportJpaEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}