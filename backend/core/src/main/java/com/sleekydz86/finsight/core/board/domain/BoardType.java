package com.sleekydz86.finsight.core.board.domain;

public enum BoardType {
    NOTICE("공지사항"),
    COMMUNITY("커뮤니티"),
    FREE("자유게시판"),
    QNA("Q&A");

    private final String description;

    BoardType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}