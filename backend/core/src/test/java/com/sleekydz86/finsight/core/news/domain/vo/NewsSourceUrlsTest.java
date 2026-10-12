package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NewsSourceUrlsTest {

    @Test
    void 끝_슬래시와_공백을_같은_주소로_맞춘다() {
        assertThat(NewsSourceUrls.normalize("  https://news.example/a/  "))
                .isEqualTo("https://news.example/a");
        assertThat(NewsSourceUrls.normalize("https://news.example"))
                .isEqualTo("https://news.example");
        assertThat(NewsSourceUrls.normalize("HTTPS://News.Example/A/"))
                .isEqualTo("https://news.example/A");
        assertThat(NewsSourceUrls.normalize(null)).isEmpty();
    }

    @Test
    void 이번_목록과_이미_저장된_주소는_한_번만_남긴다() {
        News first = news("https://news.example/a/");
        News again = news("https://news.example/a");
        News stored = news(" https://news.example/b ");
        News fresh = news("https://news.example/c");

        List<News> kept = NewsSourceUrls.withoutStoredDuplicates(
                List.of(first, again, stored, fresh),
                Set.of("https://news.example/b"));

        assertThat(kept).containsExactly(first, fresh);
        assertThat(first.getNewsMeta().getSourceUrl()).isEqualTo("https://news.example/a");
        assertThat(fresh.getNewsMeta().getSourceUrl()).isEqualTo("https://news.example/c");
    }

    @Test
    void 호스트_대소문자만_다른_주소는_한_건으로_본다() {
        News upper = news("HTTPS://News.Example/a/");
        News lower = news("https://news.example/a");

        List<News> kept = NewsSourceUrls.withoutStoredDuplicates(List.of(upper, lower), Set.of());

        assertThat(kept).containsExactly(upper);
        assertThat(upper.getNewsMeta().getSourceUrl()).isEqualTo("https://news.example/a");
    }

    private static News news(String url) {
        return News.createWithoutAI(
                NewsMeta.of(NewsProvider.YAHOO_FINANCE, url),
                new Content("제목", "본문"));
    }
}
