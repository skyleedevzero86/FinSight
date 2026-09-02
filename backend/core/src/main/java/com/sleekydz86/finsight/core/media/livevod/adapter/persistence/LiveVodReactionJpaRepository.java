package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LiveVodReactionJpaRepository extends JpaRepository<LiveVodReactionJpaEntity, Long> {
    /**
 * Finds a reaction for the specified user and video.
 *
 * @param userEmail the user's email address
 * @param videoId   the video's identifier
 * @return the matching reaction, if present
 */
Optional<LiveVodReactionJpaEntity> findByUserEmailAndVideoId(String userEmail, String videoId);

    /**
 * Counts reactions of a specific type for a video.
 *
 * @param videoId      the identifier of the video
 * @param reactionType the reaction type to count
 * @return the number of matching reactions
 */
long countByVideoIdAndReactionType(String videoId, String reactionType);

    /**
     * Groups reactions for a video by reaction type.
     *
     * @param videoId the video identifier
     * @return entries containing a reaction type and its reaction count
     */
    @Query("""
            SELECT r.reactionType, COUNT(r)
            FROM LiveVodReactionJpaEntity r
            WHERE r.videoId = :videoId
            GROUP BY r.reactionType
            """)
    java.util.List<Object[]> countGroupedByType(@Param("videoId") String videoId);
}
