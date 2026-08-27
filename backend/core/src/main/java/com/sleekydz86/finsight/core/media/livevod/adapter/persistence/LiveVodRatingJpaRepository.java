package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveVodRatingJpaRepository extends JpaRepository<LiveVodRatingJpaEntity, Long> {
    Optional<LiveVodRatingJpaEntity> findByUserEmailAndVideoId(String userEmail, String videoId);

    long countByVideoId(String videoId);

    @Query("SELECT AVG(r.stars) FROM LiveVodRatingJpaEntity r WHERE r.videoId = :videoId")
    Double averageStarsByVideoId(@Param("videoId") String videoId);

    @Query("SELECT r.videoId, COUNT(r), AVG(r.stars) FROM LiveVodRatingJpaEntity r WHERE r.videoId IN :videoIds GROUP BY r.videoId")
    List<Object[]> aggregateByVideoIds(@Param("videoIds") Collection<String> videoIds);
}
