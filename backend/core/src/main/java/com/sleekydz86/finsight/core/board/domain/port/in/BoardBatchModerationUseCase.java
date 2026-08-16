package com.sleekydz86.finsight.core.board.domain.port.in;

public interface BoardBatchModerationUseCase {

    int hideOverReportedActiveBoards(int reportThreshold);
}
