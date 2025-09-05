package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoardReactionJpaRepository extends JpaRepository<BoardReactionJpaEntity, Long> {
    Optional<BoardReactionJpaEntity> findByBoardIdAndUserEmail(Long boardId, String userEmail);

    void deleteByBoardIdAndUserEmail(Long boardId, String userEmail);

    long countByBoardIdAndReactionType(Long boardId, ReactionType reactionType);

    @Query("SELECT COUNT(br) FROM BoardReactionJpaEntity br WHERE br.boardId = :boardId AND br.reactionType = :reactionType")
    long countReactionsByBoardAndType(@Param("boardId") Long boardId,
                                      @Param("reactionType") ReactionType reactionType);
}