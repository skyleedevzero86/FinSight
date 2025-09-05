package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.Boards;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardSearchRequest;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoardRepositoryImpl implements BoardPersistencePort {

    private final BoardJpaRepository boardJpaRepository;
    private final BoardJpaMapper boardJpaMapper;

    public BoardRepositoryImpl(BoardJpaRepository boardJpaRepository, BoardJpaMapper boardJpaMapper) {
        this.boardJpaRepository = boardJpaRepository;
        this.boardJpaMapper = boardJpaMapper;
    }

    @Override
    public Board save(Board board) {
        BoardJpaEntity entity = boardJpaMapper.toEntity(board);
        BoardJpaEntity savedEntity = boardJpaRepository.save(entity);
        return boardJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Board> findById(Long boardId) {
        return boardJpaRepository.findById(boardId)
                .map(boardJpaMapper::toDomain);
    }

    @Override
    public Boards findBySearchRequest(BoardSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<BoardJpaEntity> page;

        BoardType boardType = request.getBoardType();
        String keyword = request.getKeyword();
        BoardSearchRequest.SearchType searchType = request.getSearchType();

        if (boardType == null) {
            boardType = BoardType.COMMUNITY;
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            switch (searchType) {
                case TITLE:
                    page = boardJpaRepository.findByBoardTypeAndStatusAndTitleContainingOrContentContaining(
                            boardType, BoardStatus.ACTIVE, keyword, pageable);
                    break;
                case CONTENT:
                    page = boardJpaRepository.findByBoardTypeAndStatusAndTitleContainingOrContentContaining(
                            boardType, BoardStatus.ACTIVE, keyword, pageable);
                    break;
                case HASHTAG:
                    page = boardJpaRepository.findByBoardTypeAndStatusAndHashtagsContaining(
                            boardType, BoardStatus.ACTIVE, keyword, pageable);
                    break;
                case ALL:
                default:
                    page = boardJpaRepository.findByBoardTypeAndStatusAndTitleContainingOrContentContaining(
                            boardType, BoardStatus.ACTIVE, keyword, pageable);
                    break;
            }
        } else {
            page = boardJpaRepository.findByBoardTypeAndStatusOrderByCreatedAtDesc(
                    boardType, BoardStatus.ACTIVE, pageable);
        }

        return boardJpaMapper.toDomainList(page.getContent());
    }

    @Override
    public Boards findByBoardType(BoardType boardType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BoardJpaEntity> pageResult = boardJpaRepository.findByBoardTypeAndStatusOrderByCreatedAtDesc(
                boardType, BoardStatus.ACTIVE, pageable);
        return boardJpaMapper.toDomainList(pageResult.getContent());
    }

    @Override
    public Boards findByAuthorEmail(String authorEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BoardJpaEntity> pageResult = boardJpaRepository.findByAuthorEmailAndStatusOrderByCreatedAtDesc(
                authorEmail, BoardStatus.ACTIVE, pageable);
        return boardJpaMapper.toDomainList(pageResult.getContent());
    }

    @Override
    public Boards findReportedBoards() {
        List<BoardJpaEntity> entities = boardJpaRepository.findReportedBoards(BoardStatus.ACTIVE);
        return boardJpaMapper.toDomainList(entities);
    }

    @Override
    public Boards findPopularBoards(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<BoardJpaEntity> entities = boardJpaRepository.findPopularBoards(
                BoardType.COMMUNITY, BoardStatus.ACTIVE, pageable);
        return boardJpaMapper.toDomainList(entities);
    }

    @Override
    public Boards findLatestBoards(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<BoardJpaEntity> entities = boardJpaRepository.findLatestBoards(
                BoardType.COMMUNITY, BoardStatus.ACTIVE, pageable);
        return boardJpaMapper.toDomainList(entities);
    }

    @Override
    public void deleteById(Long boardId) {
        boardJpaRepository.deleteById(boardId);
    }

    @Override
    public long countByBoardType(BoardType boardType) {
        return boardJpaRepository.countByBoardTypeAndStatus(boardType, BoardStatus.ACTIVE);
    }

    @Override
    public long countByAuthorEmail(String authorEmail) {
        return boardJpaRepository.countByAuthorEmailAndStatus(authorEmail, BoardStatus.ACTIVE);
    }

    @Override
    public List<Board> findPreviousAndNext(Long boardId, BoardType boardType) {
        Pageable pageable = PageRequest.of(0, 1);

        List<BoardJpaEntity> previous = boardJpaRepository.findPreviousBoard(
                boardType, BoardStatus.ACTIVE, boardId, pageable);
        List<BoardJpaEntity> next = boardJpaRepository.findNextBoard(
                boardType, BoardStatus.ACTIVE, boardId, pageable);

        List<Board> result = List.of();
        if (!previous.isEmpty()) {
            result = List.of(boardJpaMapper.toDomain(previous.get(0)));
        }
        if (!next.isEmpty()) {
            result = List.of(boardJpaMapper.toDomain(next.get(0)));
        }

        return result;
    }
}