package com.sleekydz86.finsight.core.comment.domain.port.out;

import com.sleekydz86.finsight.core.comment.domain.CommentReport;

import java.util.List;
import java.util.Optional;

public interface CommentReportPersistencePort {
    CommentReport save(CommentReport report);
    Optional<CommentReport> findById(Long reportId);
    List<CommentReport> findByCommentId(Long commentId);
    Optional<CommentReport> findByCommentIdAndReporterEmail(Long commentId, String reporterEmail);
    List<CommentReport> findUnprocessedReports();
    void deleteById(Long reportId);
    long countByCommentId(Long commentId);
}