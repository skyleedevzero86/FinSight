package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardType;
import com.sleekydz86.finsight.core.board.domain.BoardStatus;
import com.sleekydz86.finsight.core.board.domain.Boards;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardPersistencePort;
import com.sleekydz86.finsight.core.board.domain.port.in.dto.BoardSearchRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            return searchByKeyword(request, pageable);
        } else if (request.getHashtag() != null && !request.getHashtag().trim().isEmpty()) {
            return searchByHashtag(request, pageable);
        } else if (request.getStartDate() != null && request.getEndDate() != null) {
            return searchByDateRange(request, pageable);
        } else if (request.getBoardType() != null) {
            return searchByBoardType(request, pageable);
        } else {
            return searchByBoardType(request, pageable);
        }
    }

    @Override
    public Boards findByBoardType(BoardType boardType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        var pageResult = boardJpaRepository.findByBoardTypeAndStatusOrderByCreatedAtDesc(
                boardType, BoardStatus.ACTIVE, pageable);

        List<Board> boards = pageResult.getContent().stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, pageResult.getTotalElements());
    }

    @Override
    public Boards findByAuthorEmail(String authorEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        var pageResult = boardJpaRepository.findByAuthorEmailAndStatusOrderByCreatedAtDesc(
                authorEmail, BoardStatus.ACTIVE, pageable);

        List<Board> boards = pageResult.getContent().stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, pageResult.getTotalElements());
    }

    @Override
    public Boards findReportedBoards() {
        List<BoardJpaEntity> entities = boardJpaRepository.findReportedBoards(BoardStatus.ACTIVE);
        List<Board> boards = entities.stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, boards.size());
    }

    @Override
    public Boards findPopularBoards(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<BoardJpaEntity> entities = boardJpaRepository.findPopularBoards(
                BoardType.COMMUNITY, BoardStatus.ACTIVE, pageable);
        List<Board> boards = entities.stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, boards.size());
    }

    @Override
    public Boards findLatestBoards(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<BoardJpaEntity> entities = boardJpaRepository.findLatestBoards(
                BoardType.COMMUNITY, BoardStatus.ACTIVE, pageable);
        List<Board> boards = entities.stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, boards.size());
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
        Pageable pageable = PageRequest.of(0, 2);

        List<BoardJpaEntity> previous = boardJpaRepository.findPreviousBoard(
                boardType, BoardStatus.ACTIVE, boardId, pageable);
        List<BoardJpaEntity> next = boardJpaRepository.findNextBoard(
                boardType, BoardStatus.ACTIVE, boardId, pageable);

        List<Board> result = new java.util.ArrayList<>();
        if (!previous.isEmpty()) {
            result.add(boardJpaMapper.toDomain(previous.get(0)));
        }
        if (!next.isEmpty()) {
            result.add(boardJpaMapper.toDomain(next.get(0)));
        }

        return result;
    }

    @Override
    public void incrementViewCount(Long boardId) {
        boardJpaRepository.incrementViewCount(boardId);
    }

    private Boards searchByKeyword(BoardSearchRequest request, Pageable pageable) {
        var pageResult = boardJpaRepository.findByBoardTypeAndStatusAndTitleContainingOrContentContaining(
                request.getBoardType(), BoardStatus.ACTIVE, request.getKeyword(), pageable);

        List<Board> boards = pageResult.getContent().stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, pageResult.getTotalElements());
    }

    private Boards searchByHashtag(BoardSearchRequest request, Pageable pageable) {
        var pageResult = boardJpaRepository.findByBoardTypeAndStatusAndHashtagsContaining(
                request.getBoardType(), BoardStatus.ACTIVE, request.getHashtag(), pageable);

        List<Board> boards = pageResult.getContent().stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, pageResult.getTotalElements());
    }

    private Boards searchByDateRange(BoardSearchRequest request, Pageable pageable) {
        var pageResult = boardJpaRepository.findByBoardTypeAndStatusAndCreatedAtBetween(
                request.getBoardType(), BoardStatus.ACTIVE,
                request.getStartDate(), request.getEndDate(), pageable);

        List<Board> boards = pageResult.getContent().stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, pageResult.getTotalElements());
    }

    private Boards searchByBoardType(BoardSearchRequest request, Pageable pageable) {
        var pageResult = boardJpaRepository.findByBoardTypeAndStatusOrderByCreatedAtDesc(
                request.getBoardType(), BoardStatus.ACTIVE, pageable);

        List<Board> boards = pageResult.getContent().stream()
                .map(boardJpaMapper::toDomain)
                .toList();

        return new Boards(boards, pageResult.getTotalElements());
    }
}