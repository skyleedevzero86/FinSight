package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {

    List<CommentJpaEntity> findByTargetIdAndCommentTypeAndStatusOrderByCreatedAtDesc(
            Long targetId, CommentType commentType, CommentStatus status);

    Page<CommentJpaEntity> findByTargetIdAndCommentTypeAndStatusOrderByCreatedAtDesc(
            Long targetId, CommentType commentType, CommentStatus status, Pageable pageable);

    List<CommentJpaEntity> findByAuthorEmailAndStatusOrderByCreatedAtDesc(
            String authorEmail, CommentStatus status);

    List<CommentJpaEntity> findByParentIdAndStatusOrderByCreatedAtAsc(
            Long parentId, CommentStatus status);

    List<CommentJpaEntity> findByStatusOrderByReportCountDesc(CommentStatus status);

    @Query("SELECT c FROM CommentJpaEntity c WHERE c.status = :status AND c.reportCount > 0 ORDER BY c.reportCount DESC")
    List<CommentJpaEntity> findReportedComments(@Param("status") CommentStatus status);

    long countByTargetIdAndCommentTypeAndStatus(Long targetId, CommentType commentType, CommentStatus status);

    @Query("SELECT COUNT(c) FROM CommentJpaEntity c WHERE c.targetId = :targetId AND c.commentType = :commentType AND c.status = :status")
    long countActiveCommentsByTargetAndType(@Param("targetId") Long targetId,
                                            @Param("commentType") CommentType commentType,
                                            @Param("status") CommentStatus status);
}
