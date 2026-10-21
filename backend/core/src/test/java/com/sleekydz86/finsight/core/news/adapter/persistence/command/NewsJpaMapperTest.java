package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NewsJpaMapperTest {

    private final NewsJpaMapper mapper = new NewsJpaMapper();

    @Test
    @DisplayName("저장하고 다시 읽어도 이미지 주소를 유지한다")
    void keepsImageUrlWhenSavingAndLoading() {
        // given
        News news = newsWithImage("https://cdn.example.com/a.jpg");

        // when
        NewsJpaEntity entity = mapper.toEntity(news);
        News loaded = mapper.toDomain(entity);

        // then
        assertThat(entity.getImageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
        assertThat(loaded.getOriginalContent().getImageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
    }

    @Test
    @DisplayName("컬럼보다 긴 이미지 주소는 잘라 저장한다")
    void clipsImageUrlLongerThanColumn() {
        // given
        News news = newsWithImage("https://cdn.example.com/" + "a".repeat(1100));

        // when
        NewsJpaEntity entity = mapper.toEntity(news);

        // then
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
