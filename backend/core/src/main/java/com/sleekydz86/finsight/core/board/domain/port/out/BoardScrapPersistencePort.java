package com.sleekydz86.finsight.core.board.domain.port.out;

import com.sleekydz86.finsight.core.board.domain.BoardScrap;

import java.util.List;
import java.util.Optional;

public interface BoardScrapPersistencePort {
    BoardScrap save(BoardScrap scrap);
    Optional<BoardScrap> findByBoardIdAndUserEmail(Long boardId, String userEmail);
    void deleteByBoardIdAndUserEmail(Long boardId, String userEmail);
    List<BoardScrap> findByUserEmail(String userEmail, int page, int size);
    long countByUserEmail(String userEmail);
}