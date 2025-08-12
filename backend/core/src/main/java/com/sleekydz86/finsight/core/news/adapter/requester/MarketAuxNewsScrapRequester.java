package com.sleekydz86.finsight.core.news.adapter.requester;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.requester.MarketAuxProperties;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
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
        String formattedPublishedTimeAfter = publishTimeAfter.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        );

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(marketAuxProperties.getBaseUrl())
                        .queryParam("countries", "us")
                        .queryParam("group", "economic")
                        .queryParam("limit", limit)
                        .queryParam("published_after", formattedPublishedTimeAfter)
                        .queryParam("api_token", marketAuxProperties.getApiKey())
                        .build())
                .retrieve()
                .bodyToMono(MarketAuxResponse.class)
                .map(response -> response.getData().stream()
                        .map(newsItem -> new News(
                                0L,
                                NewsMeta.of(
                                        NewsProvider.MARKETAUX,
                                        newsItem.getPublishedAt(),
                                        newsItem.getUrl()
                                ),
                                LocalDateTime.now(),
                                new Content(
                                        newsItem.getTitle(),
                                        newsItem.getDescription()
                                ),
                                null,
                                null
                        ))
                        .collect(Collectors.toList()))
                .doOnError(WebClientResponseException.class, ex ->
                        log.warn("Failed to fetch news from Marketaux API: {} - {}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .doOnError(throwable -> !(throwable instanceof WebClientResponseException), ex ->
                        log.error("Unexpected error during Marketaux API call", ex))
                .onErrorReturn(Collections.emptyList())
                .toFuture();
    }

    public static class MarketAuxResponse {
        private List<String> warnings;
        private Meta meta;
        private List<NewsItem> data;

        public MarketAuxResponse() {}

        public MarketAuxResponse(List<String> warnings, Meta meta, List<NewsItem> data) {
            this.warnings = warnings;
            this.meta = meta;
            this.data = data;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }

        public Meta getMeta() {
            return meta;
        }

        public void setMeta(Meta meta) {
            this.meta = meta;
        }

        public List<NewsItem> getData() {
            return data;
        }

        public void setData(List<NewsItem> data) {
            this.data = data;
        }
    }

    public static class Meta {
        private int found;
        private int returned;
        private int limit;
        private int page;

        public Meta() {}

        public Meta(int found, int returned, int limit, int page) {
            this.found = found;
            this.returned = returned;
            this.limit = limit;
            this.page = page;
        }

        public int getFound() {
            return found;
        }

        public void setFound(int found) {
            this.found = found;
        }

        public int getReturned() {
            return returned;
        }

        public void setReturned(int returned) {
            this.returned = returned;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }
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

        public NewsItem() {}

        public NewsItem(String uuid, String title, String description, String keywords,
                        String snippet, String url, String imageUrl, String language,
                        String publishedAt, String source, Double relevanceScore,
                        List<Entity> entities, List<Object> similar) {
            this.uuid = uuid;
            this.title = title;
            this.description = description;
            this.keywords = keywords;
            this.snippet = snippet;
            this.url = url;
            this.imageUrl = imageUrl;
            this.language = language;
            this.publishedAt = publishedAt;
            this.source = source;
            this.relevanceScore = relevanceScore;
            this.entities = entities;
            this.similar = similar;
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

        public Entity() {}

        public Entity(String symbol, String name, String exchange, String exchangeLong,
                      String country, String type, String industry, Double matchScore,
                      Double sentimentScore, List<Highlight> highlights) {
            this.symbol = symbol;
            this.name = name;
            this.exchange = exchange;
            this.exchangeLong = exchangeLong;
            this.country = country;
            this.type = type;
            this.industry = industry;
            this.matchScore = matchScore;
            this.sentimentScore = sentimentScore;
            this.highlights = highlights;
        }

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
        private double sentiment;
        private String highlightedIn;

        public Highlight() {}

        public Highlight(String highlight, double sentiment, String highlightedIn) {
            this.highlight = highlight;
            this.sentiment = sentiment;
            this.highlightedIn = highlightedIn;
        }

        public String getHighlight() { return highlight; }
        public void setHighlight(String highlight) { this.highlight = highlight; }

        public double getSentiment() { return sentiment; }
        public void setSentiment(double sentiment) { this.sentiment = sentiment; }

        public String getHighlightedIn() { return highlightedIn; }
        public void setHighlightedIn(String highlightedIn) { this.highlightedIn = highlightedIn; }
    }
}