package com.sleekydz86.finsight.core.comment.adapter.persistence.command;

import com.sleekydz86.finsight.core.comment.domain.CommentReaction;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;
import com.sleekydz86.finsight.core.comment.domain.port.out.CommentReactionPersistencePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        if (reaction.getId() != null) {
            return commentReactionJpaRepository.findById(reaction.getId())
                    .map(existing -> {
                        existing.setReactionType(reaction.getReactionType());
                        return commentReactionJpaMapper.toDomain(commentReactionJpaRepository.save(existing));
                    })
                    .orElseGet(() -> persistNew(reaction));
        }
        return persistNew(reaction);
    }

    private CommentReaction persistNew(CommentReaction reaction) {
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

    @Override
    public List<CommentReaction> findByUserEmail(String userEmail, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return commentReactionJpaRepository
                .findByUserEmailOrderByCreatedAtDesc(userEmail, PageRequest.of(safePage, safeSize))
                .getContent()
                .stream()
                .map(commentReactionJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserEmail(String userEmail) {
        return commentReactionJpaRepository.countByUserEmail(userEmail);
    }

    @Override
    public long countByUserEmailBetween(String userEmail, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return commentReactionJpaRepository.countByUserEmailAndCreatedAtBetween(userEmail, from, to);
    }
}
