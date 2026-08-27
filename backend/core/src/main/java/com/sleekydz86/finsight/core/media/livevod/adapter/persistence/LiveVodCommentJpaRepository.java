package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveVodCommentJpaRepository extends JpaRepository<LiveVodCommentJpaEntity, Long> {
    List<LiveVodCommentJpaEntity> findByVideoIdOrderByCreatedAtAsc(String videoId);
}
