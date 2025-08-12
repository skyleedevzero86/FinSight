package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class NewsJpaMapper {

    public News toDomain(NewsJpaEntity newsJpaEntity) {
        Content translatedContent = null;
        if (newsJpaEntity.getTranslatedTitle() != null && newsJpaEntity.getTranslatedContent() != null) {
            translatedContent = new Content(
                    newsJpaEntity.getTranslatedTitle(),
                    newsJpaEntity.getTranslatedContent()
            );
        }

        AiOverview aiOverview = null;
        if (newsJpaEntity.getOverview() != null &&
                newsJpaEntity.getSentimentType() != null &&
                newsJpaEntity.getSentimentScore() != null) {
            aiOverview = new AiOverview(
                    newsJpaEntity.getOverview(),
                    newsJpaEntity.getSentimentType(),
                    newsJpaEntity.getSentimentScore(),
                    newsJpaEntity.getTargetCategories()
            );
        }

        return new News(
                newsJpaEntity.getId() != null ? newsJpaEntity.getId() : 0L,
                newsJpaEntity.getNewsProvider(),
                newsJpaEntity.getScrapedTime(),
                new Content(
                        newsJpaEntity.getOriginalTitle(),
                        newsJpaEntity.getOriginalContent()
                ),
                translatedContent,
                aiOverview
        );
    }

    public NewsJpaEntity toEntity(News news) {
        return new NewsJpaEntity(
                news.getId() == 0L ? null : news.getId(),
                news.getNewsProvider(),
                news.getScrapedTime(),
                news.getOriginalContent().getTitle(),
                news.getOriginalContent().getContent(),
                news.getTranslatedContent() != null ? news.getTranslatedContent().getTitle() : null,
                news.getTranslatedContent() != null ? news.getTranslatedContent().getContent() : null,
                news.getAiOverView() != null ? news.getAiOverView().getOverview() : null,
                news.getAiOverView() != null ? news.getAiOverView().getSentimentType() : null,
                news.getAiOverView() != null ? news.getAiOverView().getSentimentScore() : null,
                news.getAiOverView() != null ? news.getAiOverView().getTargetCategories() : java.util.Collections.emptyList()
        );
    }
}