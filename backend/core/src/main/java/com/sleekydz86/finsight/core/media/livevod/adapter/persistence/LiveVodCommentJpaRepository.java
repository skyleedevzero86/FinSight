package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LiveVodCommentJpaRepository extends JpaRepository<LiveVodCommentJpaEntity, Long> {

List<LiveVodCommentJpaEntity> findByVideoIdOrderByCreatedAtAsc(String videoId);

Page<LiveVodCommentJpaEntity> findByVideoIdAndParentIsNullOrderByCreatedAtDesc(String videoId, Pageable pageable);

Page<LiveVodCommentJpaEntity> findByParent_IdOrderByCreatedAtAsc(Long parentId, Pageable pageable);

long countByVideoId(String videoId);

long countByVideoIdAndParentIsNull(String videoId);

long countByParent_Id(Long parentId);

    @Query("SELECT c.videoId, COUNT(c) FROM LiveVodCommentJpaEntity c WHERE c.videoId IN :videoIds GROUP BY c.videoId")
    List<Object[]> countByVideoIds(@Param("videoIds") Collection<String> videoIds);
}
