package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YahooFinanceNewsScrapRequesterTest {

    @Test
    @DisplayName("RSS 항목을 뉴스로 읽는다")
    void mapsRssItem() {
        // given
        String xml = """
                <rss><channel><item>
                <title>Apple shares rise</title>
                <link>https://finance.yahoo.com/news/apple-1</link>
                <pubDate>Wed, 07 Oct 2026 09:30:00 GMT</pubDate>
                <description>Shares &lt;b&gt;climbed&lt;/b&gt;.</description>
                <enclosure url="https://cdn.example.com/a.jpg" type="image/jpeg"/>
                </item></channel></rss>
                """;
        List<News> rows = YahooFinanceNewsScrapRequester.fromRss(xml);

        // then
        assertThat(rows).hasSize(1);

        // when
        News news = rows.get(0);

        // then
        assertThat(news.getNewsProvider()).isEqualTo(NewsProvider.YAHOO_FINANCE);
        assertThat(news.getOriginalContent().getTitle()).isEqualTo("Apple shares rise");
        assertThat(news.getOriginalContent().getImageUrl()).isEqualTo("https://cdn.example.com/a.jpg");
        assertThat(news.getNewsMeta().getNewsPublishedTime()).isEqualTo(LocalDateTime.of(2026, 10, 7, 9, 30));
    }

    @Test
    @DisplayName("검색 JSON을 읽고 빈 제목은 건너뛴다")
    void mapsSearchJsonAndSkipsBlankTitle() {
        // given
        String json = """
                {"news":[
                  {"title":"","link":"https://finance.yahoo.com/x","providerPublishTime":1760000000},
                  {"title":"Nvidia climbs","link":"https://finance.yahoo.com/news/nvda",
                   "publisher":"Yahoo Finance","providerPublishTime":1760000000,
                   "thumbnail":{"resolutions":[{"url":"https://cdn.example.com/n.jpg"}]}}
                ]}
                """;
        List<News> rows = YahooFinanceNewsScrapRequester.fromSearch(json);

        // when

        // then
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getOriginalContent().getTitle()).isEqualTo("Nvidia climbs");
        assertThat(rows.get(0).getOriginalContent().getImageUrl()).isEqualTo("https://cdn.example.com/n.jpg");
    }

    @Test
    @DisplayName("끝 슬래시만 다른 주소는 한 건으로 모은다")
    void collapsesUrlsThatDifferOnlyByTrailingSlash() {
        // given
        String xml = """
                <rss><channel>
                <item>
                <title>Apple shares rise</title>
                <link>https://finance.yahoo.com/news/apple-1/</link>
                <pubDate>Wed, 07 Oct 2026 09:30:00 GMT</pubDate>
                <description>First</description>
                </item>
                <item>
                <title>Apple shares rise again</title>
                <link>https://finance.yahoo.com/news/apple-1</link>
                <pubDate>Wed, 07 Oct 2026 10:30:00 GMT</pubDate>
                <description>Second</description>
                </item>
                </channel></rss>
                """;
        List<News> rows = YahooFinanceNewsScrapRequester.fromRss(xml);

        // when

        // then
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getNewsMeta().getSourceUrl()).isEqualTo("https://finance.yahoo.com/news/apple-1");
    }
}
