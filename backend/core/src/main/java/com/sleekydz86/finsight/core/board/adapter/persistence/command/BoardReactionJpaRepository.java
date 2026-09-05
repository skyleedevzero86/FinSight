package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BoardReactionJpaRepository extends JpaRepository<BoardReactionJpaEntity, Long> {
    Optional<BoardReactionJpaEntity> findByBoardIdAndUserEmail(Long boardId, String userEmail);

    Page<BoardReactionJpaEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);

    void deleteByBoardIdAndUserEmail(Long boardId, String userEmail);

    long countByBoardIdAndReactionType(Long boardId, ReactionType reactionType);

    long countByUserEmail(String userEmail);

    @Query("""
            SELECT COUNT(br) FROM BoardReactionJpaEntity br
            WHERE br.userEmail = :userEmail
              AND br.createdAt >= :from
              AND br.createdAt < :to
            """)
    long countByUserEmailAndCreatedAtBetween(
            @Param("userEmail") String userEmail,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(br) FROM BoardReactionJpaEntity br WHERE br.boardId = :boardId AND br.reactionType = :reactionType")
    long countReactionsByBoardAndType(@Param("boardId") Long boardId,
                                      @Param("reactionType") ReactionType reactionType);
}
