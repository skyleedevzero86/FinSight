package com.sleekydz86.finsight.core.board.domain.port.in;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardFile;
import com.sleekydz86.finsight.core.board.domain.BoardScrap;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardCreateRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardReportRequest;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardUpdateRequest;

public interface BoardCommandUseCase {
    Board createBoard(String userEmail, BoardCreateRequest request);
    Board updateBoard(String userEmail, Long boardId, BoardUpdateRequest request);
    void deleteBoard(String userEmail, Long boardId);
    Board likeBoard(String userEmail, Long boardId);
    Board dislikeBoard(String userEmail, Long boardId);
    void reportBoard(String userEmail, Long boardId, BoardReportRequest request);
    void blockBoard(Long boardId);
    BoardScrap scrapBoard(String userEmail, Long boardId);
    void unscrapBoard(String userEmail, Long boardId);
    BoardFile uploadFile(Long boardId, String originalFileName, String storedFileName,
                         String filePath, String contentType, Long fileSize);
    void deleteFile(Long fileId);
}
