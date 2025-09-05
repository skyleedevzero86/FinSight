package com.sleekydz86.finsight.core.board.domain.port.out;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.Boards;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardSearchRequest;

import java.util.List;
import java.util.Optional;

public interface BoardPersistencePort {
    Board save(Board board);
    Optional<Board> findById(Long boardId);
    Boards findBySearchRequest(BoardSearchRequest request);
    Boards findByBoardType(BoardType boardType, int page, int size);
    Boards findByAuthorEmail(String authorEmail, int page, int size);
    Boards findReportedBoards();
    Boards findPopularBoards(int limit);
    Boards findLatestBoards(int limit);
    void deleteById(Long boardId);
    long countByBoardType(BoardType boardType);
    long countByAuthorEmail(String authorEmail);
    List<Board> findPreviousAndNext(Long boardId, BoardType boardType);
}