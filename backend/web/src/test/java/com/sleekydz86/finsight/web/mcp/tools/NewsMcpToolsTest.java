package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.service.NewsQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsMcpToolsTest {

    private FixedNewsQuery newsQuery;
    private NewsMcpTools tools;

    @BeforeEach
    void setUp() {
        newsQuery = new FixedNewsQuery();
        tools = new NewsMcpTools(newsQuery);
    }

    @Test
    @DisplayName("최신 뉴스는 요약과 감정을 유지한다")
    void listLatestNews_keepsOverviewAndSentiment() {
        // given

        // when
        newsQuery.latest = new Newses(List.of(sampleNews()));
        NewsMcpTools.NewsListResult result = tools.listLatestNews(null);

        // then
        assertThat(newsQuery.latestLimit).isEqualTo(10);
        assertThat(result.count()).isEqualTo(1);
        assertThat(result.items()).containsExactly(new NewsMcpTools.NewsSummary(
                7L,
                "번역 제목",
                "MARKETAUX",
                "요약",
                "POSITIVE",
                "2026-10-01T01:00",
                "https://example.com/news/7"));
    }

    @Test
    @DisplayName("최신 뉴스의 너무 큰 개수는 제한한다")
    void listLatestNews_capsOversizedLimit() {
        // given

        // when
        tools.listLatestNews(100);

        // then
        assertThat(newsQuery.latestLimit).isEqualTo(50);
    }

    @Test
    @DisplayName("알 수 없는 뉴스 카테고리는 거절한다")
    void listNewsByCategory_rejectsUnknownCategory() {
        // given

        // when

        // then
        assertThatThrownBy(() -> tools.listNewsByCategory("KOSPI", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("입력값: KOSPI")
                .hasMessageContaining("SPY");
    }

    @Test
    @DisplayName("카테고리 뉴스는 반환 개수를 제한한다")
    void listNewsByCategory_capsReturnedItems() {
        // given

        // when
        newsQuery.byCategory = new Newses(List.of(sampleNews(), sampleNews()));
        NewsMcpTools.NewsListResult result = tools.listNewsByCategory("spy", 1);

        // then
        assertThat(newsQuery.category).isEqualTo("spy");
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("뉴스 상세는 미리보기로 매핑한다")
    void getNewsDetail_mapsPreview() {
        // given

        // when
        NewsMcpTools.NewsDetailResult detail = tools.getNewsDetail(7L);

        // then
        assertThat(newsQuery.detailId).isEqualTo(7L);
        assertThat(detail.title()).isEqualTo("번역 제목");
        assertThat(detail.sentiment()).isEqualTo("POSITIVE");
        assertThat(detail.contentPreview()).isEqualTo("번역 본문");
    }

    @Test
    @DisplayName("인기 뉴스는 인기 목록에서 읽는다")
    void listPopularNews_readsPopularFeed() {
        // given

        // when
        newsQuery.popular = new Newses(List.of(sampleNews()));

        // then
        assertThat(tools.listPopularNews(3).count()).isEqualTo(1);
        assertThat(newsQuery.popularLimit).isEqualTo(3);
    }

    private static News sampleNews() {
        return new News(
                7L,
                NewsProvider.MARKETAUX,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                new Content("원문 제목", "본문"),
                new Content("번역 제목", "번역 본문"),
                new AiOverview("요약", SentimentType.POSITIVE, 0.8, List.of()),
                NewsMeta.of(NewsProvider.MARKETAUX, LocalDateTime.of(2026, 10, 1, 1, 0), "https://example.com/news/7"));
    }

    private static final class FixedNewsQuery extends NewsQueryService {
        private Newses latest = new Newses();
        private Newses popular = new Newses();
        private Newses byCategory = new Newses();
        private int latestLimit;
        private int popularLimit;
        private String category;
        private Long detailId;

        private FixedNewsQuery() {
            super(null, null, null, null, null);
        }

        @Override
        public Newses getLatestNews(int limit) {
            latestLimit = limit;
            return latest;
        }

        @Override
        public Newses getPopularNews(int limit) {
            popularLimit = limit;
            return popular;
        }

        @Override
        public Newses getNewsByCategory(String category, int limit) {
            this.category = category;
            return byCategory;
        }

        @Override
        public NewsDetailResponse getNewsDetail(Long newsId) {
            detailId = newsId;
            return new NewsDetailResponse(
                    newsId,
                    NewsProvider.MARKETAUX,
                    "원문 제목",
                    "원문 본문",
                    "번역 제목",
                    "번역 본문",
                    "요약",
                    SentimentType.POSITIVE,
                    0.8,
                    List.of(TargetCategory.SPY),
                    LocalDateTime.of(2026, 10, 1, 1, 0),
                    LocalDateTime.of(2026, 10, 1, 0, 0),
                    "https://example.com/news/7",
                    null,
                    null,
                    null,
                    null);
        }
    }
}
