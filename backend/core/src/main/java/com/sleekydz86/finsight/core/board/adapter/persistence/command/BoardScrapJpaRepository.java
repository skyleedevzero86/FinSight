package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardScrapJpaRepository extends JpaRepository<BoardScrapJpaEntity, Long> {
    Optional<BoardScrapJpaEntity> findByBoardIdAndUserEmail(Long boardId, String userEmail);

    void deleteByBoardIdAndUserEmail(Long boardId, String userEmail);

    Page<BoardScrapJpaEntity> findByUserEmailOrderByScrapedAtDesc(String userEmail, Pageable pageable);

    long countByUserEmail(String userEmail);

    @Query("SELECT COUNT(bs) FROM BoardScrapJpaEntity bs WHERE bs.userEmail = :userEmail")
    long countScrapsByUser(@Param("userEmail") String userEmail);
}