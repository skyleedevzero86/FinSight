package com.sleekydz86.finsight.core.comment.domain.port.out;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.CommentStatus;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.CommentType;

import java.util.List;
import java.util.Optional;

public interface CommentPersistencePort {
    Comment save(Comment comment);
    Optional<Comment> findById(Long commentId);
    Comments findByTargetIdAndType(Long targetId, CommentType commentType);
    Comments findByTargetIdAndTypeWithPagination(Long targetId, CommentType commentType, int page, int size);
    Comments findByUserEmail(String userEmail);
    Comments findByUserEmail(String userEmail, int page, int size);
    Comments findReportedComments();
    List<Comment> findRepliesByParentId(Long parentId);
    List<Comment> findModerationCandidates(int minReportCount);
    List<Comment> findByStatus(CommentStatus status);
    void deleteById(Long commentId);
    long countByTargetIdAndType(Long targetId, CommentType commentType);
    long countByAuthorEmail(String authorEmail);
    long countByAuthorEmailBetween(String authorEmail, java.time.LocalDateTime from, java.time.LocalDateTime to);
}
