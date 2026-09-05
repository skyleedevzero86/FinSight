package com.sleekydz86.finsight.core.board.domain.port.out;

import com.sleekydz86.finsight.core.board.domain.BoardReaction;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;

import java.util.List;
import java.util.Optional;

public interface BoardReactionPersistencePort {
    BoardReaction save(BoardReaction reaction);
    Optional<BoardReaction> findByBoardIdAndUserEmail(Long boardId, String userEmail);
    void deleteByBoardIdAndUserEmail(Long boardId, String userEmail);
    long countByBoardIdAndReactionType(Long boardId, ReactionType reactionType);
    List<BoardReaction> findByUserEmail(String userEmail, int page, int size);
    long countByUserEmail(String userEmail);
    long countByUserEmailBetween(String userEmail, java.time.LocalDateTime from, java.time.LocalDateTime to);
}