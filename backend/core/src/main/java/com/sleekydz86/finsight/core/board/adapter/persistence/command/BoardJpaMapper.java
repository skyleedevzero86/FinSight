package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.Board;
import com.sleekydz86.finsight.core.board.domain.BoardFile;
import com.sleekydz86.finsight.core.board.domain.Boards;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BoardJpaMapper {

    public Board toDomain(BoardJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        List<BoardFile> files = entity.getFiles() != null ?
                entity.getFiles().stream()
                        .map(this::toDomainFile)
                        .collect(Collectors.toList()) :
                List.of();

        return Board.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .authorEmail(entity.getAuthorEmail())
                .boardType(entity.getBoardType())
                .status(entity.getStatus())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .dislikeCount(entity.getDislikeCount())
                .commentCount(entity.getCommentCount())
                .reportCount(entity.getReportCount())
                .hashtags(entity.getHashtags())
                .files(files)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public BoardJpaEntity toEntity(Board board) {
        if (board == null) {
            return null;
        }

        List<BoardFileJpaEntity> files = board.getFiles() != null ?
                board.getFiles().stream()
                        .map(this::toEntityFile)
                        .collect(Collectors.toList()) :
                List.of();

        BoardJpaEntity entity = new BoardJpaEntity(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getAuthorEmail(),
                board.getBoardType(),
                board.getStatus(),
                board.getViewCount(),
                board.getLikeCount(),
                board.getDislikeCount(),
                board.getCommentCount(),
                board.getReportCount(),
                board.getHashtags(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
        entity.setFiles(files);
        return entity;
    }

    public Boards toDomainList(List<BoardJpaEntity> entities) {
        if (entities == null) {
            return new Boards();
        }

        List<Board> boards = entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        return new Boards(boards);
    }

    private BoardFile toDomainFile(BoardFileJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BoardFile.builder()
                .id(entity.getId())
                .boardId(entity.getBoardId())
                .originalFileName(entity.getOriginalFileName())
                .storedFileName(entity.getStoredFileName())
                .filePath(entity.getFilePath())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    private BoardFileJpaEntity toEntityFile(BoardFile file) {
        if (file == null) {
            return null;
        }

        return new BoardFileJpaEntity(
                file.getId(),
                file.getBoardId(),
                file.getOriginalFileName(),
                file.getStoredFileName(),
                file.getFilePath(),
                file.getContentType(),
                file.getFileSize(),
                file.getUploadedAt()
        );
    }
}