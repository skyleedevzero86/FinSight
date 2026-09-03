package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BoardUpdateRequest {
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private final String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 100_000, message = "내용은 100000자를 초과할 수 없습니다")
    private final String content;

    private final List<String> hashtags;

    private final BoardStatus status;

    public BoardUpdateRequest() {
        this.title = "";
        this.content = "";
        this.hashtags = List.of();
        this.status = null;
    }

    public BoardUpdateRequest(String title, String content, List<String> hashtags) {
        this(title, content, hashtags, null);
    }

    public BoardUpdateRequest(String title, String content, List<String> hashtags, BoardStatus status) {
        this.title = title;
        this.content = content;
        this.hashtags = hashtags != null ? hashtags : List.of();
        this.status = status;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getHashtags() { return hashtags; }
    public BoardStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "BoardUpdateRequest{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", hashtags=" + hashtags +
                ", status=" + status +
                '}';
    }
}