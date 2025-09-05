package com.sleekydz86.finsight.core.comment.domain.port.out;

import com.sleekydz86.finsight.core.comment.domain.Comment;
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
    Comments findReportedComments();
    List<Comment> findRepliesByParentId(Long parentId);
    void deleteById(Long commentId);
    long countByTargetIdAndType(Long targetId, CommentType commentType);
}