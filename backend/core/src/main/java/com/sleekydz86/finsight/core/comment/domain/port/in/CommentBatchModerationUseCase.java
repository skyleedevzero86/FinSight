package com.sleekydz86.finsight.core.comment.domain.port.in;

import com.sleekydz86.finsight.core.comment.domain.Comment;

import java.util.List;

public interface CommentBatchModerationUseCase {

    List<Comment> findHideCandidates(int reportThreshold);

    int hideOverReportedActiveComments(int reportThreshold, String actorEmail);

    List<Comment> findHiddenComments();

    Comment restoreHiddenComment(Long commentId);

    Comment blockComment(Long commentId);
}
