package com.sleekydz86.finsight.core.comment.domain.port.out;

import com.sleekydz86.finsight.core.comment.domain.CommentReaction;
import com.sleekydz86.finsight.core.comment.domain.ReactionType;

import java.util.List;
import java.util.Optional;

public interface CommentReactionPersistencePort {
    CommentReaction save(CommentReaction reaction);
    Optional<CommentReaction> findByCommentIdAndUserEmail(Long commentId, String userEmail);
    void deleteByCommentIdAndUserEmail(Long commentId, String userEmail);
    long countByCommentIdAndReactionType(Long commentId, ReactionType reactionType);
    List<CommentReaction> findByUserEmail(String userEmail, int page, int size);
    long countByUserEmail(String userEmail);
    long countByUserEmailBetween(String userEmail, java.time.LocalDateTime from, java.time.LocalDateTime to);
}
