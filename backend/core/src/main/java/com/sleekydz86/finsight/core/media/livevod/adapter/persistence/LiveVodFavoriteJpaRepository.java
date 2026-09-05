package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveVodFavoriteJpaRepository extends JpaRepository<LiveVodFavoriteJpaEntity, Long> {
    Optional<LiveVodFavoriteJpaEntity> findByUserEmailAndVideoId(String userEmail, String videoId);

    boolean existsByUserEmailAndVideoId(String userEmail, String videoId);

    long countByVideoId(String videoId);

    void deleteByUserEmailAndVideoId(String userEmail, String videoId);

    @Query("SELECT f.videoId, COUNT(f) FROM LiveVodFavoriteJpaEntity f WHERE f.videoId IN :videoIds GROUP BY f.videoId")
    List<Object[]> countByVideoIds(@Param("videoIds") Collection<String> videoIds);

    List<LiveVodFavoriteJpaEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Page<LiveVodFavoriteJpaEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);

    long countByUserEmail(String userEmail);
}
