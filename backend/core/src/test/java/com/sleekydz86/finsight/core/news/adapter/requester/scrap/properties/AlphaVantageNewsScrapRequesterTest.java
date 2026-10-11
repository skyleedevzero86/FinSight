package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AlphaVantageNewsScrapRequesterTest {

    @Test
    void mapsFeedItemWithImageAndPublishedTime() {
        AlphaVantageNewsScrapRequester.FeedItem item = item(
                "Apple rises",
                "Shares climbed after the report.",
                "https://cdn.example.com/a.jpg",
                "20261007T153000");

        Optional<News> news = AlphaVantageNewsScrapRequester.toNews(item);

        assertThat(news).isPresent();
        assertThat(news.orElseThrow().getNewsProvider()).isEqualTo(NewsProvider.ALPHA_VANTAGE);
        assertThat(news.orElseThrow().getOriginalContent().getTitle()).isEqualTo("Apple rises");
        assertThat(news.orElseThrow().getOriginalContent().getContent()).isEqualTo("Shares climbed after the report.");
        assertThat(news.orElseThrow().getOriginalContent().getImageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
        assertThat(news.orElseThrow().getNewsMeta().getNewsPublishedTime())
                .isEqualTo(LocalDateTime.of(2026, 10, 7, 15, 30));
    }

    @Test
    void skipsBlankTitleAndBlankImage() {
        assertThat(AlphaVantageNewsScrapRequester.toNews(item("  ", "body", null, null))).isEmpty();

        Optional<News> news = AlphaVantageNewsScrapRequester.toNews(item("Title", " ", "  ", "bad-time"));

        assertThat(news.orElseThrow().getOriginalContent().getImageUrl()).isNull();
        assertThat(news.orElseThrow().getOriginalContent().getContent()).isEqualTo("Title");
    }

    private AlphaVantageNewsScrapRequester.FeedItem item(String title, String summary, String image, String time) {
        AlphaVantageNewsScrapRequester.FeedItem item = new AlphaVantageNewsScrapRequester.FeedItem();
        item.title = title;
        item.summary = summary;
        item.url = "https://example.com/a";
        item.bannerImage = image;
        item.timePublished = time;
        return item;
    }
}
