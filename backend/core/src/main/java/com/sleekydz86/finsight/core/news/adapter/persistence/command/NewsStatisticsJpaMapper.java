package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.NewsStatistics;
import org.springframework.stereotype.Component;

@Component
public class NewsStatisticsJpaMapper {

    public NewsStatistics toDomain(NewsStatisticsJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return NewsStatistics.builder()
                .id(entity.getId())
                .newsId(entity.getNewsId())
                .viewCount(entity.getViewCount())
                .likeCount(entity.getLikeCount())
                .dislikeCount(entity.getDislikeCount())
                .commentCount(entity.getCommentCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NewsStatisticsJpaEntity toEntity(NewsStatistics statistics) {
        if (statistics == null) {
            return null;
        }

        return new NewsStatisticsJpaEntity(
                statistics.getId(),
                statistics.getNewsId(),
                statistics.getViewCount(),
                statistics.getLikeCount(),
                statistics.getDislikeCount(),
                statistics.getCommentCount(),
                statistics.getCreatedAt(),
                statistics.getUpdatedAt()
        );
    }
}