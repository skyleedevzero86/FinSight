package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardReaction;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardReactionPersistencePort;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BoardReactionRepositoryImpl implements BoardReactionPersistencePort {

    private final BoardReactionJpaRepository boardReactionJpaRepository;
    private final BoardReactionJpaMapper boardReactionJpaMapper;

    public BoardReactionRepositoryImpl(BoardReactionJpaRepository boardReactionJpaRepository,
                                       BoardReactionJpaMapper boardReactionJpaMapper) {
        this.boardReactionJpaRepository = boardReactionJpaRepository;
        this.boardReactionJpaMapper = boardReactionJpaMapper;
    }

    @Override
    public BoardReaction save(BoardReaction reaction) {
        BoardReactionJpaEntity entity = boardReactionJpaMapper.toEntity(reaction);
        BoardReactionJpaEntity savedEntity = boardReactionJpaRepository.save(entity);
        return boardReactionJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<BoardReaction> findByBoardIdAndUserEmail(Long boardId, String userEmail) {
        return boardReactionJpaRepository.findByBoardIdAndUserEmail(boardId, userEmail)
                .map(boardReactionJpaMapper::toDomain);
    }

    @Override
    public void deleteByBoardIdAndUserEmail(Long boardId, String userEmail) {
        boardReactionJpaRepository.deleteByBoardIdAndUserEmail(boardId, userEmail);
    }

    @Override
    public long countByBoardIdAndReactionType(Long boardId, ReactionType reactionType) {
        return boardReactionJpaRepository.countByBoardIdAndReactionType(boardId, reactionType);
    }
}