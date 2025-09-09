package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardScrap;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardScrapJpaMapper {

    public BoardScrap toDomain(BoardScrapJpaEntity entity) {
        return BoardScrap.builder()
                .id(entity.getId())
                .boardId(entity.getBoardId())
                .userEmail(entity.getUserEmail())
                .scrapedAt(entity.getScrapedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public BoardScrapJpaEntity toEntity(BoardScrap scrap) {
        BoardScrapJpaEntity entity = new BoardScrapJpaEntity();
        entity.setId(scrap.getId());
        entity.setBoardId(scrap.getBoardId());
        entity.setUserEmail(scrap.getUserEmail());
        entity.setScrapedAt(scrap.getScrapedAt());
        entity.setCreatedAt(scrap.getCreatedAt());
        entity.setUpdatedAt(scrap.getUpdatedAt());
        return entity;
    }

    public List<BoardScrap> toDomainList(List<BoardScrapJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}