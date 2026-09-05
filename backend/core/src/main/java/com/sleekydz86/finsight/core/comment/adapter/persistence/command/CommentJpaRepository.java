package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {

    List<CommentJpaEntity> findByTargetIdAndCommentTypeAndStatusAndParentIdIsNullOrderByCreatedAtDesc(
            Long targetId, CommentType commentType, CommentStatus status);

    Page<CommentJpaEntity> findByTargetIdAndCommentTypeAndStatusAndParentIdIsNullOrderByCreatedAtDesc(
            Long targetId, CommentType commentType, CommentStatus status, Pageable pageable);

    List<CommentJpaEntity> findByTargetIdAndCommentTypeAndStatusOrderByCreatedAtDesc(
            Long targetId, CommentType commentType, CommentStatus status);

    Page<CommentJpaEntity> findByTargetIdAndCommentTypeAndStatusOrderByCreatedAtDesc(
            Long targetId, CommentType commentType, CommentStatus status, Pageable pageable);

    List<CommentJpaEntity> findByAuthorEmailAndStatusOrderByCreatedAtDesc(
            String authorEmail, CommentStatus status);

    Page<CommentJpaEntity> findByAuthorEmailAndStatusOrderByCreatedAtDesc(
            String authorEmail, CommentStatus status, Pageable pageable);

    long countByAuthorEmailAndStatus(String authorEmail, CommentStatus status);

    @Query("""
            SELECT COUNT(c) FROM CommentJpaEntity c
            WHERE c.authorEmail = :authorEmail
              AND c.status = :status
              AND c.createdAt >= :from
              AND c.createdAt < :to
            """)
    long countByAuthorEmailAndStatusAndCreatedAtBetween(
            @Param("authorEmail") String authorEmail,
            @Param("status") CommentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    List<CommentJpaEntity> findByParentIdAndStatusOrderByCreatedAtAsc(
            Long parentId, CommentStatus status);

    List<CommentJpaEntity> findByStatusOrderByReportCountDesc(CommentStatus status);

    @Query("SELECT c FROM CommentJpaEntity c WHERE c.status = :status AND c.reportCount > 0 ORDER BY c.reportCount DESC")
    List<CommentJpaEntity> findReportedComments(@Param("status") CommentStatus status);

    @Query("""
            SELECT c FROM CommentJpaEntity c
            WHERE c.targetId = :targetId
              AND c.commentType = :commentType
              AND c.parentId IS NULL
              AND c.status IN :statuses
            ORDER BY c.createdAt DESC
            """)
    List<CommentJpaEntity> findVisibleRoots(
            @Param("targetId") Long targetId,
            @Param("commentType") CommentType commentType,
            @Param("statuses") List<CommentStatus> statuses);

    @Query("""
            SELECT c FROM CommentJpaEntity c
            WHERE c.targetId = :targetId
              AND c.commentType = :commentType
              AND c.parentId IS NULL
              AND c.status IN :statuses
            ORDER BY c.createdAt DESC
            """)
    Page<CommentJpaEntity> findVisibleRoots(
            @Param("targetId") Long targetId,
            @Param("commentType") CommentType commentType,
            @Param("statuses") List<CommentStatus> statuses,
            Pageable pageable);

    @Query("""
            SELECT c FROM CommentJpaEntity c
            WHERE c.parentId = :parentId
              AND c.status IN :statuses
            ORDER BY c.createdAt ASC
            """)
    List<CommentJpaEntity> findVisibleReplies(
            @Param("parentId") Long parentId,
            @Param("statuses") List<CommentStatus> statuses);

    @Query("""
            SELECT c FROM CommentJpaEntity c
            WHERE c.status = :status
              AND c.reportCount >= :minReportCount
            ORDER BY c.reportCount DESC, c.id DESC
            """)
    List<CommentJpaEntity> findModerationCandidates(
            @Param("status") CommentStatus status,
            @Param("minReportCount") int minReportCount);

    List<CommentJpaEntity> findByStatusOrderByUpdatedAtDesc(CommentStatus status);

    long countByTargetIdAndCommentTypeAndStatus(Long targetId, CommentType commentType, CommentStatus status);

    @Query("SELECT COUNT(c) FROM CommentJpaEntity c WHERE c.targetId = :targetId AND c.commentType = :commentType AND c.status = :status")
    long countActiveCommentsByTargetAndType(@Param("targetId") Long targetId,
                                            @Param("commentType") CommentType commentType,
                                            @Param("status") CommentStatus status);

    @Query(value = """
            SELECT DATE(`createdAt`) AS d, COUNT(*) AS c
            FROM comments
            WHERE `createdAt` >= :from AND `createdAt` < :to
            GROUP BY DATE(`createdAt`)
            ORDER BY d
            """, nativeQuery = true)
    List<Object[]> countCreatedByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
