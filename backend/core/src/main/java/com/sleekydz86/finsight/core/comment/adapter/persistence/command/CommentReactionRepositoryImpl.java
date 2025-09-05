package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentReaction;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReactionPersistencePort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CommentReactionRepositoryImpl implements CommentReactionPersistencePort {

    private final CommentReactionJpaRepository commentReactionJpaRepository;
    private final CommentReactionJpaMapper commentReactionJpaMapper;

    public CommentReactionRepositoryImpl(CommentReactionJpaRepository commentReactionJpaRepository,
                                         CommentReactionJpaMapper commentReactionJpaMapper) {
        this.commentReactionJpaRepository = commentReactionJpaRepository;
        this.commentReactionJpaMapper = commentReactionJpaMapper;
    }

    @Override
    public CommentReaction save(CommentReaction reaction) {
        CommentReactionJpaEntity entity = commentReactionJpaMapper.toEntity(reaction);
        CommentReactionJpaEntity savedEntity = commentReactionJpaRepository.save(entity);
        return commentReactionJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<CommentReaction> findByCommentIdAndUserEmail(Long commentId, String userEmail) {
        return commentReactionJpaRepository.findByCommentIdAndUserEmail(commentId, userEmail)
                .map(commentReactionJpaMapper::toDomain);
    }

    @Override
    public void deleteByCommentIdAndUserEmail(Long commentId, String userEmail) {
        commentReactionJpaRepository.deleteByCommentIdAndUserEmail(commentId, userEmail);
    }

    @Override
    public long countByCommentIdAndReactionType(Long commentId, ReactionType reactionType) {
        return commentReactionJpaRepository.countByCommentIdAndReactionType(commentId, reactionType);
    }
}