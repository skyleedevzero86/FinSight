package com.sleekydz86.finsight.core.comment.domain.port.out;

import com.sleekydz86.finsight.core.comment.domain.CommentReaction;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;

import java.util.Optional;

public interface CommentReactionPersistencePort {
    CommentReaction save(CommentReaction reaction);
    Optional<CommentReaction> findByCommentIdAndUserEmail(Long commentId, String userEmail);
    void deleteByCommentIdAndUserEmail(Long commentId, String userEmail);
    long countByCommentIdAndReactionType(Long commentId, ReactionType reactionType);
}