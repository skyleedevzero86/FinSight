package com.sleekydz86.finsight.core.board.domain;

public record BoardModerationTarget(
        Long boardId,
        String title,
        String authorEmail,
        String boardType,
        int reportCount) {
}
