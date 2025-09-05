package com.sleekydz86.finsight.core.global.exception;

public class CommentNotFoundException extends BaseException {
    private final Long commentId;

    public CommentNotFoundException(Long commentId) {
        super(
                String.format("댓글을 찾을 수 없습니다. ID: %d", commentId),
                "COMMENT_NOT_FOUND",
                "COMMENT",
                404
        );
        this.commentId = commentId;
    }

    public CommentNotFoundException(Long commentId, String message) {
        super(
                String.format("댓글을 찾을 수 없습니다. ID: %d, 메시지: %s", commentId, message),
                "COMMENT_NOT_FOUND",
                "COMMENT",
                404
        );
        this.commentId = commentId;
    }

    public CommentNotFoundException(Long commentId, String message, Throwable cause) {
        super(
                String.format("댓글을 찾을 수 없습니다. ID: %d, 메시지: %s", commentId, message),
                "COMMENT_NOT_FOUND",
                "COMMENT",
                404,
                cause
        );
        this.commentId = commentId;
    }

    public Long getCommentId() {
        return commentId;
    }
}