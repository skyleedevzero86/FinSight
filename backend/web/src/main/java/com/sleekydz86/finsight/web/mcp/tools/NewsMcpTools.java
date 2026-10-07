package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.vo.NewsTextRepair;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.news.service.NewsQueryService;
import com.sleekydz86.finsight.web.mcp.McpText;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "spring.ai.mcp.server", name = "enabled", havingValue = "true")
public class NewsMcpTools {

    private final NewsQueryService newsQueryService;

    public NewsMcpTools(NewsQueryService newsQueryService) {
        this.newsQueryService = newsQueryService;
    }

    @Tool(description = "최신 수집 뉴스 목록을 조회합니다. 읽기 전용입니다.")
    public NewsListResult listLatestNews(
            @ToolParam(description = "조회 개수 (1~50)", required = false) Integer limit) {
        return toResult(newsQueryService.getLatestNews(clamp(limit, 1, 50, 10)));
    }

    @Tool(description = "인기/최신 정렬 기준 뉴스 목록을 조회합니다. 읽기 전용입니다.")
    public NewsListResult listPopularNews(
            @ToolParam(description = "조회 개수 (1~50)", required = false) Integer limit) {
        return toResult(newsQueryService.getPopularNews(clamp(limit, 1, 50, 10)));
    }

    @Tool(description = "종목·자산 분류별 뉴스를 조회합니다. category 예: SPY, QQQ, BTC, AAPL, MSFT, NVDA, GOOGL, META, TSLA, NONE. 읽기 전용입니다.")
    public NewsListResult listNewsByCategory(
            @ToolParam(description = "종목 분류. SPY, QQQ, BTC, AAPL, MSFT, NVDA, GOOGL, META, TSLA, GENERAL, BITCOIN, NONE") String category,
            @ToolParam(description = "조회 개수 (1~50)", required = false) Integer limit) {
        int safeLimit = clamp(limit, 1, 50, 10);
        requireCategory(category);
        return toResult(capped(newsQueryService.getNewsByCategory(category, safeLimit), safeLimit));
    }

    @Tool(description = "뉴스 제목·본문을 검색합니다. 읽기 전용입니다.")
    public NewsListResult searchNews(
            @ToolParam(description = "검색어", required = false) String keyword,
            @ToolParam(description = "종목 분류. 생략 가능", required = false) String category,
            @ToolParam(description = "페이지 번호 (0부터)", required = false) Integer page,
            @ToolParam(description = "페이지 크기 (1~20)", required = false) Integer size) {
        NewsSearchRequest request = new NewsSearchRequest();
        request.setKeyword(blank(keyword));
        request.setCategories(categoryFilter(category));
        request.setPage(page == null || page < 0 ? 0 : page);
        request.setSize(clamp(size, 1, 20, 10));
        return toResult(flatten(newsQueryService.searchNews(request)));
    }

    @Tool(description = "뉴스 상세를 조회합니다. 화면 상세와 같이 조회수가 증가합니다. 읽기 전용 수정은 없습니다.")
    public NewsDetailResult getNewsDetail(
            @ToolParam(description = "뉴스 ID") Long newsId) {
        requireId(newsId, "뉴스 ID");
        NewsDetailResponse detail = newsQueryService.getNewsDetail(newsId);
        String title = first(detail.getTranslatedTitle(), detail.getOriginalTitle());
        String body = first(detail.getTranslatedContent(), detail.getOriginalContent());
        return new NewsDetailResult(
                detail.getId(),
                NewsTextRepair.repair(title),
                detail.getNewsProvider() == null ? null : detail.getNewsProvider().name(),
                NewsTextRepair.repair(detail.getOverview()),
                detail.getSentimentType() == null ? null : detail.getSentimentType().name(),
                text(detail.getPublishedTime()),
                detail.getSourceUrl(),
                McpText.clip(NewsTextRepair.repair(body), 1200));
    }

    @Tool(description = "관련 뉴스 목록을 조회합니다. 읽기 전용입니다.")
    public NewsListResult listRelatedNews(
            @ToolParam(description = "기준 뉴스 ID") Long newsId,
            @ToolParam(description = "조회 개수 (1~20)", required = false) Integer limit) {
        requireId(newsId, "뉴스 ID");
        return toResult(newsQueryService.getRelatedNews(newsId, clamp(limit, 1, 20, 5)));
    }

    private static void requireId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(name + "가 올바르지 않습니다. 입력값: " + id + ". 1 이상의 숫자여야 합니다.");
        }
    }

    private static void requireCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("종목 분류가 필요합니다. 허용: " + categories());
        }
        try {
            TargetCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "종목 분류가 올바르지 않습니다. 입력값: " + category + ". 허용: " + categories(),
                    exception);
        }
    }

    private static List<TargetCategory> categoryFilter(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        requireCategory(category);
        return List.of(TargetCategory.valueOf(category.trim().toUpperCase()));
    }

    private static String categories() {
        return String.join(", ", Arrays.stream(TargetCategory.values()).map(Enum::name).toList());
    }

    private static Newses capped(Newses newses, int limit) {
        if (newses == null || newses.getNewses() == null) {
            return new Newses();
        }
        return new Newses(newses.getNewses().stream().limit(limit).toList());
    }

    private static Newses flatten(PaginationResponse<Newses> page) {
        if (page == null || page.getContent() == null) {
            return new Newses();
        }
        List<News> items = page.getContent().stream()
                .filter(group -> group != null && group.getNewses() != null)
                .flatMap(group -> group.getNewses().stream())
                .toList();
        return new Newses(items);
    }

    private static String blank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String first(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private NewsListResult toResult(Newses newses) {
        List<NewsSummary> items = newses == null || newses.getNewses() == null
                ? List.of()
                : newses.getNewses().stream().map(this::toSummary).toList();
        return new NewsListResult(items.size(), items);
    }

    private NewsSummary toSummary(News news) {
        return new NewsSummary(
                news.getId(),
                title(news),
                news.getNewsProvider() == null ? null : news.getNewsProvider().name(),
                overview(news),
                sentiment(news),
                text(publishedAt(news)),
                news.getNewsMeta() == null ? null : news.getNewsMeta().getSourceUrl());
    }

    private static String title(News news) {
        String translated = news.getTranslatedContent() == null ? null : news.getTranslatedContent().getTitle();
        String original = news.getOriginalContent() == null ? null : news.getOriginalContent().getTitle();
        if (translated != null) {
            return NewsTextRepair.repair(translated);
        }
        return NewsTextRepair.repair(original);
    }

    private static String overview(News news) {
        if (news.getAiOverView() == null) {
            return null;
        }
        return NewsTextRepair.repair(news.getAiOverView().getOverview());
    }

    private static String sentiment(News news) {
        if (news.getAiOverView() == null || news.getAiOverView().getSentimentType() == null) {
            return null;
        }
        return news.getAiOverView().getSentimentType().name();
    }

    private static LocalDateTime publishedAt(News news) {
        if (news.getNewsMeta() != null && news.getNewsMeta().getPublishedTime() != null) {
            return news.getNewsMeta().getPublishedTime();
        }
        return news.getScrapedTime();
    }

    private static String text(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private static int clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.min(Math.max(value, min), max);
    }

    public record NewsSummary(
            Long id,
            String title,
            String provider,
            String overview,
            String sentiment,
            String publishedAt,
            String sourceUrl) {
    }

    public record NewsListResult(int count, List<NewsSummary> items) {
    }

    public record NewsDetailResult(
            Long id,
            String title,
            String provider,
            String overview,
            String sentiment,
            String publishedAt,
            String sourceUrl,
            String contentPreview) {
    }
}
