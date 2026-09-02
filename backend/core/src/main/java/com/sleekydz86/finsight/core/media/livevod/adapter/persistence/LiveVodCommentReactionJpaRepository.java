package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveVodCommentReactionJpaRepository extends JpaRepository<LiveVodCommentReactionJpaEntity, Long> {
    /**
 * Finds a user's reaction for a comment.
 *
 * @param userEmail the user's email address
 * @param commentId the comment identifier
 * @return the user's reaction for the comment, if one exists
 */
Optional<LiveVodCommentReactionJpaEntity> findByUserEmailAndCommentId(String userEmail, Long commentId);

    /**
 * Counts reactions of a specified type for a comment.
 *
 * @param commentId   the comment identifier
 * @param reactionType the reaction type to count
 * @return the number of matching reactions
 */
long countByCommentIdAndReactionType(Long commentId, String reactionType);

    /**
     * Counts reactions grouped by comment ID and reaction type.
     *
     * @param commentIds the comment IDs to include
     * @return rows containing the comment ID, reaction type, and reaction count
     */
    @Query("""
            SELECT r.commentId, r.reactionType, COUNT(r)
            FROM LiveVodCommentReactionJpaEntity r
            WHERE r.commentId IN :commentIds
            GROUP BY r.commentId, r.reactionType
            """)
    List<Object[]> countGroupedByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    /**
 * Finds a user's reactions for the specified comment IDs.
 *
 * @param userEmail  the user's email address
 * @param commentIds the comment IDs to search
 * @return the user's reactions associated with the specified comments
 */
List<LiveVodCommentReactionJpaEntity> findByUserEmailAndCommentIdIn(String userEmail, Collection<Long> commentIds);
}
