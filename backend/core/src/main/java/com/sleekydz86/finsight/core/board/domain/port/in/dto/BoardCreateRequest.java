package com.sleekydz86.finsight.core.board.domain.port.in.dto;

import com.sleekydz86.finsight.core.board.domain.BoardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BoardCreateRequest {
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private final String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10000자를 초과할 수 없습니다")
    private final String content;

    @NotNull(message = "게시판 타입은 필수입니다")
    private final BoardType boardType;

    private final List<String> hashtags;

    public BoardCreateRequest() {
        this.title = "";
        this.content = "";
        this.boardType = BoardType.COMMUNITY;
        this.hashtags = List.of();
    }

    public BoardCreateRequest(String title, String content, BoardType boardType, List<String> hashtags) {
        this.title = title;
        this.content = content;
        this.boardType = boardType;
        this.hashtags = hashtags != null ? hashtags : List.of();
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public BoardType getBoardType() { return boardType; }
    public List<String> getHashtags() { return hashtags; }

    @Override
    public String toString() {
        return "BoardCreateRequest{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", boardType=" + boardType +
                ", hashtags=" + hashtags +
                '}';
    }
}
