package com.sleekydz86.finsight.core.board.domain.port.out;

import com.sleekydz86.finsight.core.board.domain.BoardFile;

import java.util.List;
import java.util.Optional;

public interface BoardFilePersistencePort {
    BoardFile save(BoardFile file);
    Optional<BoardFile> findById(Long fileId);
    List<BoardFile> findByBoardId(Long boardId);
    void deleteById(Long fileId);
    void deleteByBoardId(Long boardId);
}