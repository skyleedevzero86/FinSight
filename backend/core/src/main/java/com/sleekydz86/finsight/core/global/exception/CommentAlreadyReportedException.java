package com.sleekydz86.finsight.core.global.exception;

public class CommentAlreadyReportedException extends BaseException {
    private final Long commentId;
    private final String userEmail;

    public CommentAlreadyReportedException(Long commentId, String userEmail) {
        super("이미 신고한 댓글입니다: " + commentId, "COMMENT_ALREADY_REPORTED", "COMMENT", 400);
        this.commentId = commentId;
        this.userEmail = userEmail;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getUserEmail() {
        return userEmail;
    }
}