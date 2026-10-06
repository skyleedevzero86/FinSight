package com.sleekydz86.finsight.core.media.youtube.adapter.persistence.command;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface YoutubeImportSourceJpaRepository extends JpaRepository<YoutubeImportSourceJpaEntity, Long> {
    List<YoutubeImportSourceJpaEntity> findAllByOrderByCreatedAtDesc();

    List<YoutubeImportSourceJpaEntity> findByActiveTrueOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM YoutubeImportSourceJpaEntity e WHERE e.id = :id")
    Optional<YoutubeImportSourceJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
