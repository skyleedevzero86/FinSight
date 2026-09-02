package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveVodCommentReactionJpaRepository extends JpaRepository<LiveVodCommentReactionJpaEntity, Long> {
    Optional<LiveVodCommentReactionJpaEntity> findByUserEmailAndCommentId(String userEmail, Long commentId);

    long countByCommentIdAndReactionType(Long commentId, String reactionType);

    @Query("""
            SELECT r.commentId, r.reactionType, COUNT(r)
            FROM LiveVodCommentReactionJpaEntity r
            WHERE r.commentId IN :commentIds
            GROUP BY r.commentId, r.reactionType
            """)
    List<Object[]> countGroupedByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    List<LiveVodCommentReactionJpaEntity> findByUserEmailAndCommentIdIn(String userEmail, Collection<Long> commentIds);
}
