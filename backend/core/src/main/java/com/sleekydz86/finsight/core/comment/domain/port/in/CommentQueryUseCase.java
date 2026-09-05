package com.sleekydz86.finsight.core.comment.domain.port.in;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.comment.domain.CommentType;
import com.sleekydz86.finsight.core.comment.domain.port.in.dto.CommentResponse;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;

import java.util.List;
import java.util.Map;

public interface CommentQueryUseCase {
    Comments getCommentsByTargetId(Long targetId, CommentType commentType);
    Comments getCommentsByTargetIdWithPagination(Long targetId, CommentType commentType, int page, int size);
    Comment getCommentById(Long commentId);
    List<CommentResponse> getCommentReplies(Long parentId);
    Comments getCommentsByUser(String userEmail);
    Comments getReportedComments();
    boolean hasUserLikedComment(String userEmail, Long commentId);
    boolean hasUserDislikedComment(String userEmail, Long commentId);
    PaginationResponse<Map<String, Object>> getCommentsByUserEmail(String userEmail, int page, int size);
    PaginationResponse<Map<String, Object>> getMyReactions(String userEmail, int page, int size);
}
