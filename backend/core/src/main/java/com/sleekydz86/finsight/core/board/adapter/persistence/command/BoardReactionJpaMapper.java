package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardReaction;
import org.springframework.stereotype.Component;

@Component
public class BoardReactionJpaMapper {

    public BoardReaction toDomain(BoardReactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BoardReaction.builder()
                .id(entity.getId())
                .boardId(entity.getBoardId())
                .userEmail(entity.getUserEmail())
                .reactionType(entity.getReactionType())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public BoardReactionJpaEntity toEntity(BoardReaction reaction) {
        if (reaction == null) {
            return null;
        }

        return new BoardReactionJpaEntity(
                reaction.getId(),
                reaction.getBoardId(),
                reaction.getUserEmail(),
                reaction.getReactionType(),
                reaction.getCreatedAt()
        );
    }
}