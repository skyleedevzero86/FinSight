package com.sleekydz86.finsight.core.global.exception;

public class BoardNotFoundException extends BaseException {
    private final Long boardId;

    public BoardNotFoundException(Long boardId) {
        super("게시판을 찾을 수 없습니다. ID: " + boardId, "BOARD_001", "BOARD", 404);
        this.boardId = boardId;
    }

    public BoardNotFoundException(Long boardId, String message) {
        super("게시판을 찾을 수 없습니다. ID: " + boardId + ", 메시지: " + message, "BOARD_001", "BOARD", 404);
        this.boardId = boardId;
    }

    public BoardNotFoundException(Long boardId, String message, Throwable cause) {
        super("게시판을 찾을 수 없습니다. ID: " + boardId + ", 메시지: " + message, "BOARD_001", "BOARD", 404, cause);
        this.boardId = boardId;
    }

    public Long getBoardId() {
        return boardId;
    }
}