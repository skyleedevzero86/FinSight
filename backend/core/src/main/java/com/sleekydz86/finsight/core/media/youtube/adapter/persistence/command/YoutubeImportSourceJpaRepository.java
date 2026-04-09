package com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YoutubeImportSourceJpaRepository extends JpaRepository<YoutubeImportSourceJpaEntity, Long> {
    List<YoutubeImportSourceJpaEntity> findAllByOrderByCreatedAtDesc();

    List<YoutubeImportSourceJpaEntity> findByActiveTrueOrderByCreatedAtDesc();
}
