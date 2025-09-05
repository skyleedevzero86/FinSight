package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReportJpaRepository extends JpaRepository<CommentReportJpaEntity, Long> {

    List<CommentReportJpaEntity> findByCommentIdOrderByReportedAtDesc(Long commentId);

    Optional<CommentReportJpaEntity> findByCommentIdAndReporterEmail(Long commentId, String reporterEmail);

    List<CommentReportJpaEntity> findByIsProcessedFalseOrderByReportedAtAsc();

    long countByCommentId(Long commentId);

    @Query("SELECT COUNT(cr) FROM CommentReportJpaEntity cr WHERE cr.commentId = :commentId")
    long countReportsByComment(@Param("commentId") Long commentId);
}