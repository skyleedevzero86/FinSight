package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentReport;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentReportJpaMapper {

    public CommentReport toDomain(CommentReportJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return CommentReport.builder()
                .id(entity.getId())
                .commentId(entity.getCommentId())
                .reporterEmail(entity.getReporterEmail())
                .reason(entity.getReason())
                .description(entity.getDescription())
                .reportedAt(entity.getReportedAt())
                .isProcessed(entity.isProcessed())
                .build();
    }

    public CommentReportJpaEntity toEntity(CommentReport report) {
        if (report == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        return new CommentReportJpaEntity(
                report.getId(),
                report.getCommentId(),
                report.getReporterEmail(),
                report.getReason(),
                report.getDescription(),
                report.getReportedAt(),
                report.isProcessed(),
                now,
                now
        );
    }

    public List<CommentReport> toDomainList(List<CommentReportJpaEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}