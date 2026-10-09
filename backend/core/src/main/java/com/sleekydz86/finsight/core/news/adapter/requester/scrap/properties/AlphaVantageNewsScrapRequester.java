package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class AlphaVantageNewsScrapRequester implements NewsScrapRequester {

    static final String TICKERS = "SPY,QQQ,CRYPTO:BTC,AAPL,MSFT,NVDA,GOOGL,META,TSLA";
    private static final DateTimeFormatter PUBLISHED = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final Logger log = LoggerFactory.getLogger(AlphaVantageNewsScrapRequester.class);

    private final WebClient webClient;
    private final AlphaVantageProperties properties;

    public AlphaVantageNewsScrapRequester(WebClient webClient, AlphaVantageProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    @Override
    public NewsProvider supports() {
        return NewsProvider.ALPHA_VANTAGE;
    }

    @Override
    public CompletableFuture<List<News>> scrap(LocalDateTime publishTimeAfter, int limit) {
        if (!properties.isConfigured()) {
            log.warn("Alpha Vantage API 키가 없어 뉴스 수집을 건너뜁니다. ALPHAVANTAGE_API_KEY를 설정하세요.");
            return CompletableFuture.completedFuture(List.of());
        }
        var uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .queryParam("function", "NEWS_SENTIMENT")
                .queryParam("tickers", TICKERS)
                .queryParam("sort", "LATEST")
                .queryParam("limit", bounded(limit))
                .queryParam("apikey", properties.getApiKey())
                .build()
                .toUri();
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(AlphaVantageResponse.class)
                .map(response -> convert(response, publishTimeAfter))
                .doOnError(error -> log.error("Alpha Vantage 뉴스 조회 실패: {}", error.getMessage()))
                .onErrorReturn(List.of())
                .toFuture();
    }

    private int bounded(int limit) {
        return Math.min(Math.max(limit, 1), 50);
    }

    private List<News> convert(AlphaVantageResponse response, LocalDateTime after) {
        if (response == null || response.feed == null) {
            return List.of();
        }
        return response.feed.stream()
                .map(AlphaVantageNewsScrapRequester::toNews)
                .flatMap(Optional::stream)
                .filter(news -> !news.getNewsMeta().getNewsPublishedTime().isBefore(after))
                .toList();
    }

    static Optional<News> toNews(FeedItem item) {
        if (item == null || item.title == null || item.title.isBlank()) {
            return Optional.empty();
        }
        String url = item.url == null || item.url.isBlank() ? "https://www.alphavantage.co" : item.url;
        NewsMeta meta = NewsMeta.of(NewsProvider.ALPHA_VANTAGE, publishedAt(item.timePublished), url);
        String body = item.summary == null || item.summary.isBlank() ? item.title : item.summary;
        Content content = new Content(item.title, body).withImageUrl(blankToNull(item.bannerImage));
        return Optional.of(News.createWithoutAI(meta, content));
    }

    private static LocalDateTime publishedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(raw.trim(), PUBLISHED);
        } catch (DateTimeParseException exception) {
            return LocalDateTime.now();
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlphaVantageResponse {
        @JsonProperty("feed")
        public List<FeedItem> feed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeedItem {
        public String title;
        public String url;
        public String summary;
        @JsonProperty("banner_image")
        public String bannerImage;
        @JsonProperty("time_published")
        public String timePublished;
    }
}
