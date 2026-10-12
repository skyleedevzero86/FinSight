package com.sleekydz86.finsight.core.economypick.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooChartClient {

    private static final Logger log = LoggerFactory.getLogger(YahooChartClient.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public YahooChartClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public List<PriceBar> daily(String symbol) {
        return chart(symbol, "3mo", "1d");
    }

    public List<PriceBar> chart(String symbol, String range, String interval) {
        try {
            String body = webClient.get()
                    .uri(chartUri(symbol, range, interval))
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(7));
            return parse(body);
        } catch (WebClientResponseException exception) {
            log.warn("야후 시세 조회에 실패했습니다. symbol={} status={}", symbol, exception.getStatusCode().value());
            return List.of();
        } catch (RuntimeException exception) {
            log.warn("야후 시세 조회에 실패했습니다. symbol={}", symbol, exception);
            return List.of();
        }
    }

    public URI chartUri(String symbol) {
        return chartUri(symbol, "3mo", "1d");
    }

    public URI chartUri(String symbol, String range, String interval) {
        return UriComponentsBuilder.fromUriString("https://query1.finance.yahoo.com")
                .pathSegment("v8", "finance", "chart", symbol)
                .queryParam("range", range)
                .queryParam("interval", interval)
                .encode()
                .build()
                .toUri();
    }

    public List<PriceBar> parse(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode quote = objectMapper.readTree(body).path("chart").path("result").path(0);
            JsonNode stamps = quote.path("timestamp");
            JsonNode prices = quote.path("indicators").path("quote").path(0);
            return bars(stamps, prices);
        } catch (Exception exception) {
            log.warn("야후 시세 응답을 해석하지 못했습니다.", exception);
            return List.of();
        }
    }

    private List<PriceBar> bars(JsonNode stamps, JsonNode prices) {
        List<PriceBar> rows = new ArrayList<>();
        if (!stamps.isArray()) {
            return rows;
        }
        for (int index = 0; index < stamps.size(); index++) {
            PriceBar bar = bar(stamps.get(index), prices, index);
            if (bar != null) {
                rows.add(bar);
            }
        }
        return rows;
    }

    private PriceBar bar(JsonNode stamp, JsonNode prices, int index) {
        Double close = number(prices.path("close").path(index));
        if (close == null) {
            return null;
        }
        String date = Instant.ofEpochSecond(stamp.asLong()).atOffset(ZoneOffset.UTC).toLocalDate().format(DAY);
        double open = value(prices.path("open").path(index), close);
        double high = value(prices.path("high").path(index), close);
        double low = value(prices.path("low").path(index), close);
        double volume = value(prices.path("volume").path(index), 0);
        return new PriceBar(date, open, high, low, close, volume);
    }

    private Double number(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode() || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }

    private double value(JsonNode node, double fallback) {
        Double parsed = number(node);
        return parsed == null ? fallback : parsed;
    }
}
