package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentReport;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReportPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CommentReportRepositoryImpl implements CommentReportPersistencePort {

    private final CommentReportJpaRepository commentReportJpaRepository;
    private final CommentReportJpaMapper commentReportJpaMapper;

    public CommentReportRepositoryImpl(CommentReportJpaRepository commentReportJpaRepository,
                                       CommentReportJpaMapper commentReportJpaMapper) {
        this.commentReportJpaRepository = commentReportJpaRepository;
        this.commentReportJpaMapper = commentReportJpaMapper;
    }

    @Override
    public CommentReport save(CommentReport report) {
        CommentReportJpaEntity entity = commentReportJpaMapper.toEntity(report);
        CommentReportJpaEntity savedEntity = commentReportJpaRepository.save(entity);
        return commentReportJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<CommentReport> findById(Long reportId) {
        return commentReportJpaRepository.findById(reportId)
                .map(commentReportJpaMapper::toDomain);
    }

    @Override
    public List<CommentReport> findByCommentId(Long commentId) {
        List<CommentReportJpaEntity> entities = commentReportJpaRepository
                .findByCommentIdOrderByReportedAtDesc(commentId);
        return commentReportJpaMapper.toDomainList(entities);
    }

    @Override
    public Optional<CommentReport> findByCommentIdAndReporterEmail(Long commentId, String reporterEmail) {
        return commentReportJpaRepository.findByCommentIdAndReporterEmail(commentId, reporterEmail)
                .map(commentReportJpaMapper::toDomain);
    }

    @Override
    public List<CommentReport> findUnprocessedReports() {
        List<CommentReportJpaEntity> entities = commentReportJpaRepository
                .findByIsProcessedFalseOrderByReportedAtAsc();
        return commentReportJpaMapper.toDomainList(entities);
    }

    @Override
    public void deleteById(Long reportId) {
        commentReportJpaRepository.deleteById(reportId);
    }

    @Override
    public long countByCommentId(Long commentId) {
        return commentReportJpaRepository.countByCommentId(commentId);
    }
}
