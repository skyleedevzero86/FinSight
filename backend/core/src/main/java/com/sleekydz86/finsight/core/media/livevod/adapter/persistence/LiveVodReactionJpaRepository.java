package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LiveVodReactionJpaRepository extends JpaRepository<LiveVodReactionJpaEntity, Long> {
    Optional<LiveVodReactionJpaEntity> findByUserEmailAndVideoId(String userEmail, String videoId);

    long countByVideoIdAndReactionType(String videoId, String reactionType);

    @Query("""
            SELECT r.reactionType, COUNT(r)
            FROM LiveVodReactionJpaEntity r
            WHERE r.videoId = :videoId
            GROUP BY r.reactionType
            """)
    java.util.List<Object[]> countGroupedByType(@Param("videoId") String videoId);
}
