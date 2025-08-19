package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MarketAuxSentimentRequester {

    private final Logger log = LoggerFactory.getLogger(MarketAuxSentimentRequester.class);
    private final WebClient webClient;
    private final MarketAuxProperties marketAuxProperties;

    public MarketAuxSentimentRequester(WebClient webClient, MarketAuxProperties marketAuxProperties) {
        this.webClient = webClient;
        this.marketAuxProperties = marketAuxProperties;
    }

    public Mono<SentimentAggregationResponse> getSentimentAggregation(LocalDateTime publishedAfter) {
        String formattedTime = publishedAfter.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

        String url = String.format("%s/entity/stats/aggregation?countries=us&group_by=country&limit=10&published_after=%s&api_token=%s",
                marketAuxProperties.getBaseUrl().replace("/news/all", ""),
                formattedTime,
                marketAuxProperties.getApiKey());

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(SentimentAggregationResponse.class)
                .doOnError(error -> log.error("Failed to fetch sentiment aggregation: {}", error.getMessage()));
    }

    public static class SentimentAggregationResponse {
        private Meta meta;
        private List<SentimentData> data;

        public Meta getMeta() { return meta; }
        public void setMeta(Meta meta) { this.meta = meta; }
        public List<SentimentData> getData() { return data; }
        public void setData(List<SentimentData> data) { this.data = data; }
    }

    public static class Meta {
        private int returned;
        private int limit;

        public int getReturned() { return returned; }
        public void setReturned(int returned) { this.returned = returned; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }

    public static class SentimentData {
        private String key;
        private int totalDocuments;
        private double sentimentAvg;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public int getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }
        public double getSentimentAvg() { return sentimentAvg; }
        public void setSentimentAvg(double sentimentAvg) { this.sentimentAvg = sentimentAvg; }
    }
}