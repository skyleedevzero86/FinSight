package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NewsSourceUrlsTest {

    @Test
    @DisplayName("끝 슬래시와 공백을 같은 주소로 맞춘다")
    void treatsTrailingSlashAndSpaceAsTheSameUrl() {
        // given

        // when

        // then
        assertThat(NewsSourceUrls.normalize("  https://news.example/a/  "))
                .isEqualTo("https://news.example/a");
        assertThat(NewsSourceUrls.normalize("https://news.example"))
                .isEqualTo("https://news.example");
        assertThat(NewsSourceUrls.normalize("HTTPS://News.Example/A/"))
                .isEqualTo("https://news.example/A");
        assertThat(NewsSourceUrls.normalize(null)).isEmpty();
    }

    @Test
    @DisplayName("이번 목록과 이미 저장된 주소는 한 번만 남긴다")
    void keepsCurrentAndStoredUrlsOnce() {
        // given
        News first = news("https://news.example/a/");
        News again = news("https://news.example/a");
        News stored = news(" https://news.example/b ");
        News fresh = news("https://news.example/c");
        List<News> kept = NewsSourceUrls.withoutStoredDuplicates(
                List.of(first, again, stored, fresh),
                Set.of("https://news.example/b"));

        // when

        // then
        assertThat(kept).containsExactly(first, fresh);
        assertThat(first.getNewsMeta().getSourceUrl()).isEqualTo("https://news.example/a");
        assertThat(fresh.getNewsMeta().getSourceUrl()).isEqualTo("https://news.example/c");
    }

    @Test
    @DisplayName("호스트 대소문자만 다른 주소는 한 건으로 본다")
    void treatsHostCaseDifferenceAsOneUrl() {
        // given
        News upper = news("HTTPS://News.Example/a/");
        News lower = news("https://news.example/a");
        List<News> kept = NewsSourceUrls.withoutStoredDuplicates(List.of(upper, lower), Set.of());

        // when

        // then
        assertThat(kept).containsExactly(upper);
        assertThat(upper.getNewsMeta().getSourceUrl()).isEqualTo("https://news.example/a");
    }

    private static News news(String url) {
        return News.createWithoutAI(
                NewsMeta.of(NewsProvider.YAHOO_FINANCE, url),
                new Content("제목", "본문"));
    }
}
