package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardModerationRunJpaRepository extends JpaRepository<BoardModerationRunJpaEntity, Long> {

    @Query(value = """
            SELECT * FROM board_moderation_run
            ORDER BY created_at DESC, id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<BoardModerationRunJpaEntity> findRecent(@Param("limit") int limit);
}
