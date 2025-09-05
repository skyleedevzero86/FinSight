package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardFile;
import com.sleekydz86.finsight.core.board.domain.port.out.BoardFilePersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoardFileRepositoryImpl implements BoardFilePersistencePort {

    private final BoardFileJpaRepository boardFileJpaRepository;

    public BoardFileRepositoryImpl(BoardFileJpaRepository boardFileJpaRepository) {
        this.boardFileJpaRepository = boardFileJpaRepository;
    }

    @Override
    public BoardFile save(BoardFile file) {
        BoardFileJpaEntity entity = new BoardFileJpaEntity(
                file.getId(),
                file.getBoardId(),
                file.getOriginalFileName(),
                file.getStoredFileName(),
                file.getFilePath(),
                file.getContentType(),
                file.getFileSize(),
                file.getUploadedAt()
        );
        BoardFileJpaEntity savedEntity = boardFileJpaRepository.save(entity);

        return BoardFile.builder()
                .id(savedEntity.getId())
                .boardId(savedEntity.getBoardId())
                .originalFileName(savedEntity.getOriginalFileName())
                .storedFileName(savedEntity.getStoredFileName())
                .filePath(savedEntity.getFilePath())
                .contentType(savedEntity.getContentType())
                .fileSize(savedEntity.getFileSize())
                .uploadedAt(savedEntity.getUploadedAt())
                .build();
    }

    @Override
    public Optional<BoardFile> findById(Long fileId) {
        return boardFileJpaRepository.findById(fileId)
                .map(entity -> BoardFile.builder()
                        .id(entity.getId())
                        .boardId(entity.getBoardId())
                        .originalFileName(entity.getOriginalFileName())
                        .storedFileName(entity.getStoredFileName())
                        .filePath(entity.getFilePath())
                        .contentType(entity.getContentType())
                        .fileSize(entity.getFileSize())
                        .uploadedAt(entity.getUploadedAt())
                        .build());
    }

    @Override
    public List<BoardFile> findByBoardId(Long boardId) {
        List<BoardFileJpaEntity> entities = boardFileJpaRepository.findByBoardId(boardId);
        return entities.stream()
                .map(entity -> BoardFile.builder()
                        .id(entity.getId())
                        .boardId(entity.getBoardId())
                        .originalFileName(entity.getOriginalFileName())
                        .storedFileName(entity.getStoredFileName())
                        .filePath(entity.getFilePath())
                        .contentType(entity.getContentType())
                        .fileSize(entity.getFileSize())
                        .uploadedAt(entity.getUploadedAt())
                        .build())
                .toList();
    }

    @Override
    public void deleteById(Long fileId) {
        boardFileJpaRepository.deleteById(fileId);
    }

    @Override
    public void deleteByBoardId(Long boardId) {
        boardFileJpaRepository.deleteByBoardId(boardId);
    }
}