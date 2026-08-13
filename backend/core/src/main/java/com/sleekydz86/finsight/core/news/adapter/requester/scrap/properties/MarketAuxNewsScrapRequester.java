package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class MarketAuxNewsScrapRequester implements NewsScrapRequester {

    private static final Logger log = LoggerFactory.getLogger(MarketAuxNewsScrapRequester.class);

    private final WebClient webClient;
    private final MarketAuxProperties marketAuxProperties;

    public MarketAuxNewsScrapRequester(WebClient webClient, MarketAuxProperties marketAuxProperties) {
        this.webClient = webClient;
        this.marketAuxProperties = marketAuxProperties;
    }

    @Override
    public NewsProvider supports() {
        return NewsProvider.MARKETAUX;
    }

    @Override
    public CompletableFuture<List<News>> scrap(LocalDateTime publishTimeAfter, int limit) {
        if (!marketAuxProperties.isConfigured()) {
            log.warn("MarketAux API 키가 없어 뉴스 수집을 건너뜁니다. MARKETAUX_API_KEY를 설정하세요.");
            return CompletableFuture.completedFuture(List.of());
        }
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(marketAuxProperties.getBaseUrl())
                        .queryParam("api_token", marketAuxProperties.getApiKey())
                        .queryParam("limit", limit)
                        .queryParam("published_after", publishTimeAfter.toInstant(ZoneOffset.UTC))
                        .build())
                .retrieve()
                .bodyToMono(MarketAuxResponse.class)
                .map(this::convertToNews)
                .doOnError(error -> log.error("MarketAux에서 뉴스 조회 실패: {}", error.getMessage()))
                .onErrorReturn(List.of())
                .toFuture();
    }

    private List<News> convertToNews(MarketAuxResponse response) {
        if (response == null || response.data == null) {
            return List.of();
        }

        return response.data.stream()
                .map(this::convertToNews)
                .toList();
    }

    private News convertToNews(MarketAuxNewsItem item) {
        try {
            OffsetDateTime publishedTime = OffsetDateTime.parse(item.published_at);
            NewsMeta newsMeta = NewsMeta.of(
                    NewsProvider.MARKETAUX,
                    publishedTime.toLocalDateTime(),
                    item.url);

            Content originalContent = new Content(item.title, item.description);

            return News.createWithoutAI(newsMeta, originalContent);
        } catch (Exception e) {
            log.error("MarketAux 뉴스 항목 변환 실패: {}", e.getMessage());

            NewsMeta fallbackMeta = NewsMeta.of(
                    NewsProvider.MARKETAUX,
                    LocalDateTime.now(),
                    item.url != null ? item.url : "https://api.marketaux.com");
            Content fallbackContent = new Content(
                    item.title != null ? item.title : "제목 없음",
                    item.description != null ? item.description : "설명 없음");

            return News.createWithoutAI(fallbackMeta, fallbackContent);
        }
    }

    public static class MarketAuxResponse {
        @JsonProperty("data")
        public List<MarketAuxNewsItem> data;

        @JsonProperty("meta")
        public MarketAuxMeta meta;
    }

    public static class MarketAuxNewsItem {
        @JsonProperty("title")
        public String title;

        @JsonProperty("description")
        public String description;

        @JsonProperty("url")
        public String url;

        @JsonProperty("published_at")
        public String published_at;

        @JsonProperty("source")
        public String source;

        @JsonProperty("sentiment")
        public String sentiment;
    }

    public static class MarketAuxMeta {
        @JsonProperty("found")
        public int found;

        @JsonProperty("returned")
        public int returned;

        @JsonProperty("limit")
        public int limit;

        @JsonProperty("page")
        public int page;
    }
}