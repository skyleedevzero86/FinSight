package com.sleekydz86.finsight.core.comment.domain.port.in;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentCreateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentUpdateRequest;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentReportRequest;

public interface CommentCommandUseCase {
    Comment createComment(String userEmail, CommentCreateRequest request);
    Comment updateComment(String userEmail, Long commentId, CommentUpdateRequest request);
    void deleteComment(String userEmail, Long commentId);
    Comment likeComment(String userEmail, Long commentId);
    Comment dislikeComment(String userEmail, Long commentId);
    void reportComment(String userEmail, Long commentId, CommentReportRequest request);
    void blockComment(Long commentId);
}
