package com.sleekydz86.finsight.core.board.domain.port.in;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.global.dto.PaginationResponse;

import java.util.List;

public interface BoardQueryUseCase {
    PaginationResponse<BoardListResponse> getBoards(BoardSearchRequest request);
    BoardDetailResponse getBoardDetail(Long boardId);
    BoardDetailResponse getBoardDetailWithNavigation(Long boardId, BoardType boardType);
    List<BoardListResponse> getPopularBoards(int limit);
    List<BoardListResponse> getLatestBoards(int limit);
    List<BoardListResponse> getBoardsByCategory(BoardType boardType, int limit);
    List<BoardListResponse> getMyScrappedBoards(String userEmail, int page, int size);
    List<BoardListResponse> getMyBoards(String userEmail, int page, int size);
    List<BoardListResponse> getReportedBoards();
    boolean hasUserLikedBoard(String userEmail, Long boardId);
    boolean hasUserDislikedBoard(String userEmail, Long boardId);
    boolean hasUserScrappedBoard(String userEmail, Long boardId);
    BoardStatisticsResponse getBoardStatistics();
    List<BoardAuthorStatisticsResponse> getAuthorStatistics();
}
