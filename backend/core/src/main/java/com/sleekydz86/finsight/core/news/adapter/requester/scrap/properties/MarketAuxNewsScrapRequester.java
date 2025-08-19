package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class MarketAuxNewsScrapRequester implements NewsScrapRequester {

    private final Logger log = LoggerFactory.getLogger(MarketAuxNewsScrapRequester.class);
    private final WebClient webClient;
    private final MarketAuxProperties marketAuxProperties;

    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

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
        ZonedDateTime estZoneDateTime = publishTimeAfter.atZone(ZoneId.of("America/New_York"));
        ZonedDateTime utcZoneDateTime = estZoneDateTime.withZoneSameInstant(ZoneId.of("UTC"));
        String formattedPublishedTimeAfter = utcZoneDateTime.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        return webClient.get()
                .uri(URI.create(
                        marketAuxProperties.getBaseUrl() + "?" +
                                "countries=us&" +
                                "filter_entities=true&" +
                                "limit=" + limit + "&" +
                                "published_after=" + formattedPublishedTimeAfter + "&" +
                                "api_token=" + marketAuxProperties.getApiKey()))
                .retrieve()
                .bodyToMono(MarketAuxResponse.class)
                .map(response -> response.getData().stream()
                        .map(this::convertToNews)
                        .collect(Collectors.toList()))
                .doOnError(WebClientResponseException.class,
                        ex -> log.warn("Failed to fetch news from Marketaux API: {} - {}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .doOnError(throwable -> !(throwable instanceof WebClientResponseException),
                        ex -> log.error("Unexpected error during Marketaux API call", ex))
                .onErrorReturn(Collections.emptyList())
                .toFuture();
    }

    private News convertToNews(NewsItem newsItem) {

        SentimentType sentimentType = convertSentimentScore(newsItem.getAverageSentimentScore());

        List<TargetCategory> categories = extractTargetCategories(newsItem.getEntities());

        return News.createWithoutAI(
                NewsMeta.of(
                        NewsProvider.MARKETAUX,
                        OffsetDateTime.parse(newsItem.getPublishedAt(), API_DATE_FORMATTER),
                        newsItem.getUrl()
                ),
                new Content(
                        newsItem.getTitle(),
                        newsItem.getDescription()
                )
        );
    }

    private SentimentType convertSentimentScore(Double sentimentScore) {
        if (sentimentScore == null) {
            return SentimentType.NEUTRAL;
        }
        
        if (sentimentScore > 0.1) {
            return SentimentType.POSITIVE;
        } else if (sentimentScore < -0.1) {
            return SentimentType.NEGATIVE;
        } else {
            return SentimentType.NEUTRAL;
        }
    }

    private List<TargetCategory> extractTargetCategories(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of(TargetCategory.NONE);
        }

        return entities.stream()
                .map(Entity::getSymbol)
                .filter(symbol -> symbol != null && !symbol.isEmpty())
                .map(this::mapSymbolToCategory)
                .filter(category -> category != TargetCategory.NONE)
                .distinct()
                .collect(Collectors.toList());
    }

    private TargetCategory mapSymbolToCategory(String symbol) {
        if (symbol == null) return TargetCategory.NONE;
        
        return switch (symbol.toUpperCase()) {
            case "SPY" -> TargetCategory.SPY;
            case "QQQ" -> TargetCategory.QQQ;
            case "BTC" -> TargetCategory.BTC;
            case "AAPL" -> TargetCategory.AAPL;
            case "MSFT" -> TargetCategory.MSFT;
            case "NVDA" -> TargetCategory.NVDA;
            case "GOOGL" -> TargetCategory.GOOGL;
            case "META" -> TargetCategory.META;
            case "TSLA" -> TargetCategory.TSLA;
            default -> TargetCategory.NONE;
        };
    }

    public static class MarketAuxResponse {
        private List<String> warnings;
        private Meta meta;
        private List<NewsItem> data;

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public Meta getMeta() { return meta; }
        public void setMeta(Meta meta) { this.meta = meta; }
        public List<NewsItem> getData() { return data; }
        public void setData(List<NewsItem> data) { this.data = data; }
    }

    public static class Meta {
        private int found;
        private int returned;
        private int limit;
        private int page;

        public int getFound() { return found; }
        public void setFound(int found) { this.found = found; }
        public int getReturned() { return returned; }
        public void setReturned(int returned) { this.returned = returned; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
    }

    public static class NewsItem {
        private String uuid;
        private String title;
        private String description;
        private String keywords;
        private String snippet;
        private String url;
        private String imageUrl;
        private String language;
        private String publishedAt;
        private String source;
        private Double relevanceScore;
        private List<Entity> entities;
        private List<Object> similar;

        public Double getAverageSentimentScore() {
            if (entities == null || entities.isEmpty()) {
                return null;
            }
            
            return entities.stream()
                    .mapToDouble(entity -> entity.getSentimentScore() != null ? entity.getSentimentScore() : 0.0)
                    .average()
                    .orElse(0.0);
        }

        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getPublishedAt() { return publishedAt; }
        public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public Double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(Double relevanceScore) { this.relevanceScore = relevanceScore; }
        public List<Entity> getEntities() { return entities; }
        public void setEntities(List<Entity> entities) { this.entities = entities; }
        public List<Object> getSimilar() { return similar; }
        public void setSimilar(List<Object> similar) { this.similar = similar; }
    }

    public static class Entity {
        private String symbol;
        private String name;
        private String exchange;
        private String exchangeLong;
        private String country;
        private String type;
        private String industry;
        private Double matchScore;
        private Double sentimentScore;
        private List<Highlight> highlights;

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getExchange() { return exchange; }
        public void setExchange(String exchange) { this.exchange = exchange; }
        public String getExchangeLong() { return exchangeLong; }
        public void setExchangeLong(String exchangeLong) { this.exchangeLong = exchangeLong; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getIndustry() { return industry; }
        public void setIndustry(String industry) { this.industry = industry; }
        public Double getMatchScore() { return matchScore; }
        public void setMatchScore(Double matchScore) { this.matchScore = matchScore; }
        public Double getSentimentScore() { return sentimentScore; }
        public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
        public List<Highlight> getHighlights() { return highlights; }
        public void setHighlights(List<Highlight> highlights) { this.highlights = highlights; }
    }

    public static class Highlight {
        private String highlight;
        private Double sentiment;
        private String highlightedIn;

        public String getHighlight() { return highlight; }
        public void setHighlight(String highlight) { this.highlight = highlight; }
        public Double getSentiment() { return sentiment; }
        public void setSentiment(Double sentiment) { this.sentiment = sentiment; }
        public String getHighlightedIn() { return highlightedIn; }
        public void setHighlightedIn(String highlightedIn) { this.highlightedIn = highlightedIn; }
    }
}