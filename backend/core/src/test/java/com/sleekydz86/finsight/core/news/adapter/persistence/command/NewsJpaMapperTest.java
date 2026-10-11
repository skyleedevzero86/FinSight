package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NewsJpaMapperTest {

    private final NewsJpaMapper mapper = new NewsJpaMapper();

    @Test
    void keepsImageUrlWhenSavingAndLoading() {
        News news = newsWithImage("https://cdn.example.com/a.jpg");

        NewsJpaEntity entity = mapper.toEntity(news);
        News loaded = mapper.toDomain(entity);

        assertThat(entity.getImageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
        assertThat(loaded.getOriginalContent().getImageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
    }

    @Test
    void clipsImageUrlLongerThanColumn() {
        News news = newsWithImage("https://cdn.example.com/" + "a".repeat(1100));

        NewsJpaEntity entity = mapper.toEntity(news);

        assertThat(entity.getImageUrl()).hasSize(1000);
    }

    private News newsWithImage(String imageUrl) {
        NewsMeta meta = NewsMeta.of(
                NewsProvider.MARKETAUX,
                LocalDateTime.of(2026, 10, 7, 9, 0),
                "https://example.com/article");
        Content content = new Content("제목", "본문").withImageUrl(imageUrl);
        return News.createWithoutAI(meta, content);
    }
}
