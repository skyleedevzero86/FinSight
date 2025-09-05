package com.sleekydz86.finsight.core.comment.domain.port.in.dto;

public class CommentUpdateRequest {
    private final String content;

    public CommentUpdateRequest() {
        this.content = null;
    }

    public CommentUpdateRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }

    @Override
    public String toString() {
        return "CommentUpdateRequest{" +
                "content='" + content + '\'' +
                '}';
    }
}