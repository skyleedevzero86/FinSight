package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardScrap;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardScrapPersistencePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoardScrapRepositoryImpl implements BoardScrapPersistencePort {

    private final BoardScrapJpaRepository boardScrapJpaRepository;
    private final BoardScrapJpaMapper boardScrapJpaMapper;

    public BoardScrapRepositoryImpl(BoardScrapJpaRepository boardScrapJpaRepository,
            BoardScrapJpaMapper boardScrapJpaMapper) {
        this.boardScrapJpaRepository = boardScrapJpaRepository;
        this.boardScrapJpaMapper = boardScrapJpaMapper;
    }

    @Override
    public BoardScrap save(BoardScrap scrap) {
        BoardScrapJpaEntity entity = boardScrapJpaMapper.toEntity(scrap);
        BoardScrapJpaEntity savedEntity = boardScrapJpaRepository.save(entity);
        return boardScrapJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<BoardScrap> findByBoardIdAndUserEmail(Long boardId, String userEmail) {
        return boardScrapJpaRepository.findByBoardIdAndUserEmail(boardId, userEmail)
                .map(boardScrapJpaMapper::toDomain);
    }

    @Override
    public void deleteByBoardIdAndUserEmail(Long boardId, String userEmail) {
        boardScrapJpaRepository.deleteByBoardIdAndUserEmail(boardId, userEmail);
    }

    @Override
    public List<BoardScrap> findByUserEmail(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return boardScrapJpaRepository.findByUserEmailOrderByScrapedAtDesc(userEmail, pageable)
                .stream()
                .map(boardScrapJpaMapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserEmail(String userEmail) {
        return boardScrapJpaRepository.countByUserEmail(userEmail);
    }
}