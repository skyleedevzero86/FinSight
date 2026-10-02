package com.sleekydz86.finsight.web.mcp.tools;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.service.NewsQueryService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
        int safeLimit = clamp(limit, 1, 50, 10);
        Newses newses = newsQueryService.getLatestNews(safeLimit);
        List<NewsSummary> items = newses.getNewses().stream()
                .map(this::toSummary)
                .toList();
        return new NewsListResult(items.size(), items);
    }

    @Tool(description = "인기/최신 정렬 기준 뉴스 목록을 조회합니다. 읽기 전용입니다.")
    public NewsListResult listPopularNews(
            @ToolParam(description = "조회 개수 (1~50)", required = false) Integer limit) {
        int safeLimit = clamp(limit, 1, 50, 10);
        Newses newses = newsQueryService.getPopularNews(safeLimit);
        List<NewsSummary> items = newses.getNewses().stream()
                .map(this::toSummary)
                .toList();
        return new NewsListResult(items.size(), items);
    }

    private NewsSummary toSummary(News news) {
        String title = null;
        if (news.getTranslatedContent() != null && news.getTranslatedContent().getTitle() != null) {
            title = news.getTranslatedContent().getTitle();
        } else if (news.getOriginalContent() != null) {
            title = news.getOriginalContent().getTitle();
        }
        String overview = news.getAiOverView() != null ? news.getAiOverView().getOverview() : null;
        String sentiment = news.getAiOverView() != null && news.getAiOverView().getSentimentType() != null
                ? news.getAiOverView().getSentimentType().name()
                : null;
        String provider = news.getNewsProvider() != null ? news.getNewsProvider().name() : null;
        return new NewsSummary(news.getId(), title, provider, overview, sentiment);
    }

    private static int clamp(Integer value, int min, int max, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Math.min(Math.max(value, min), max);
    }

    public record NewsSummary(Long id, String title, String provider, String overview, String sentiment) {
    }

    public record NewsListResult(int count, List<NewsSummary> items) {
    }
}
