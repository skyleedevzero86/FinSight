package com.sleekydz86.finsight.core.board.adapter.persistence.command;

import com.sleekydz86.finsight.core.board.domain.BoardScrap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BoardScrapJpaMapper {

    public BoardScrap toDomain(BoardScrapJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return BoardScrap.builder()
                .id(entity.getId())
                .boardId(entity.getBoardId())
                .userEmail(entity.getUserEmail())
                .scrapedAt(entity.getScrapedAt())
                .build();
    }

    public BoardScrapJpaEntity toEntity(BoardScrap scrap) {
        if (scrap == null) {
            return null;
        }

        return new BoardScrapJpaEntity(
                scrap.getId(),
                scrap.getBoardId(),
                scrap.getUserEmail(),
                scrap.getScrapedAt(),
                scrap.getScrapedAt()
        );
    }

    public List<BoardScrap> toDomainList(List<BoardScrapJpaEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}