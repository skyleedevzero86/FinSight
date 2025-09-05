package com.sleekydz86.finsight.core.comment.domain.port.in.dto;

import com.sleekydz86.finsight.core.comment.domain.CommentType;

public class CommentCreateRequest {
    private final String content;
    private final CommentType commentType;
    private final Long targetId;
    private final Long parentId;

    public CommentCreateRequest() {
        this.content = null;
        this.commentType = null;
        this.targetId = null;
        this.parentId = null;
    }

    public CommentCreateRequest(String content, CommentType commentType, Long targetId, Long parentId) {
        this.content = content;
        this.commentType = commentType;
        this.targetId = targetId;
        this.parentId = parentId;
    }

    public boolean isReply() {
        return parentId != null;
    }

    public String getContent() { return content; }
    public CommentType getCommentType() { return commentType; }
    public Long getTargetId() { return targetId; }
    public Long getParentId() { return parentId; }

    @Override
    public String toString() {
        return "CommentCreateRequest{" +
                "content='" + content + '\'' +
                ", commentType=" + commentType +
                ", targetId=" + targetId +
                ", parentId=" + parentId +
                '}';
    }
}
