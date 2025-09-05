package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardFileJpaRepository extends JpaRepository<BoardFileJpaEntity, Long> {
    List<BoardFileJpaEntity> findByBoardId(Long boardId);

    @Modifying
    @Query("DELETE FROM BoardFileJpaEntity bf WHERE bf.boardId = :boardId")
    void deleteByBoardId(@Param("boardId") Long boardId);
}