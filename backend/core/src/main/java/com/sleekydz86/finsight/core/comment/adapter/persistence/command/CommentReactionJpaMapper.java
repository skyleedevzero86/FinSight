package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentReaction;
import org.springframework.stereotype.Component;

@Component
public class CommentReactionJpaMapper {

    public CommentReaction toDomain(CommentReactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return CommentReaction.builder()
                .id(entity.getId())
                .commentId(entity.getCommentId())
                .userEmail(entity.getUserEmail())
                .reactionType(entity.getReactionType())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CommentReactionJpaEntity toEntity(CommentReaction reaction) {
        if (reaction == null) {
            return null;
        }

        return new CommentReactionJpaEntity(
                reaction.getId(),
                reaction.getCommentId(),
                reaction.getUserEmail(),
                reaction.getReactionType(),
                reaction.getCreatedAt()
        );
    }
}