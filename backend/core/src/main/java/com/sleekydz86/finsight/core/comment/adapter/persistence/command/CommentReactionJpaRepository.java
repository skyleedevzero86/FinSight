package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CommentReactionJpaRepository extends JpaRepository<CommentReactionJpaEntity, Long> {

    Optional<CommentReactionJpaEntity> findByCommentIdAndUserEmail(Long commentId, String userEmail);

    void deleteByCommentIdAndUserEmail(Long commentId, String userEmail);

    long countByCommentIdAndReactionType(Long commentId, ReactionType reactionType);

    long countByUserEmail(String userEmail);

    @Query("""
            SELECT COUNT(cr) FROM CommentReactionJpaEntity cr
            WHERE cr.userEmail = :userEmail
              AND cr.createdAt >= :from
              AND cr.createdAt < :to
            """)
    long countByUserEmailAndCreatedAtBetween(
            @Param("userEmail") String userEmail,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(cr) FROM CommentReactionJpaEntity cr WHERE cr.commentId = :commentId AND cr.reactionType = :reactionType")
    long countReactionsByCommentAndType(@Param("commentId") Long commentId,
                                        @Param("reactionType") ReactionType reactionType);

    Page<CommentReactionJpaEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);
}
