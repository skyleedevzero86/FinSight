package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaRepository;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class YahooFinanceNewsScrapRequester implements NewsScrapRequester {

    static final String TICKERS = "SPY,QQQ,BTC-USD,AAPL,MSFT,NVDA,GOOGL,META,TSLA";
    private static final String RSS = "https://feeds.finance.yahoo.com/rss/2.0/headline";
    private static final String SEARCH = "https://query1.finance.yahoo.com/v1/finance/search";
    private static final Logger log = LoggerFactory.getLogger(YahooFinanceNewsScrapRequester.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final WebClient webClient;
    private final NewsJpaRepository newsJpaRepository;

    public YahooFinanceNewsScrapRequester(WebClient webClient, NewsJpaRepository newsJpaRepository) {
        this.webClient = webClient;
        this.newsJpaRepository = newsJpaRepository;
    }

    @Override
    public NewsProvider supports() {
        return NewsProvider.YAHOO_FINANCE;
    }

    @Override
    public CompletableFuture<List<News>> scrap(LocalDateTime publishTimeAfter, int limit) {
        return fetch(rssUrl())
                .map(YahooFinanceNewsScrapRequester::fromRss)
                .onErrorResume(error -> searchAfterRssFailure(error))
                .map(items -> keepFresh(items, publishTimeAfter, limit))
                .onErrorReturn(List.of())
                .toFuture();
    }

    private Mono<List<News>> searchAfterRssFailure(Throwable error) {
        log.warn("Yahoo Finance RSS 조회 실패: {}", error.getMessage());
        return fetch(searchUrl()).map(YahooFinanceNewsScrapRequester::fromSearch);
    }

    private Mono<String> fetch(String url) {
        return webClient.get()
                .uri(url)
                .header("User-Agent", "FinSight/1.0")
                .retrieve()
                .bodyToMono(String.class);
    }

    private String rssUrl() {
        return RSS + "?s=" + TICKERS + "&region=US&lang=en-US";
    }

    private String searchUrl() {
        return SEARCH + "?q=stock%20market&quotesCount=0&newsCount=12";
    }

    private List<News> keepFresh(List<News> items, LocalDateTime after, int limit) {
        int bounded = Math.min(Math.max(limit, 1), 20);
        List<News> fresh = new ArrayList<>();
        for (News news : items) {
            if (fresh.size() >= bounded) {
                break;
            }
            if (news.getNewsMeta().getNewsPublishedTime().isBefore(after)) {
                continue;
            }
            String url = news.getNewsMeta().getSourceUrl();
            if (newsJpaRepository.existsBySourceUrl(url)) {
                continue;
            }
            fresh.add(news);
        }
        return fresh;
    }

    static List<News> fromRss(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        LinkedHashMap<String, News> unique = new LinkedHashMap<>();
        for (Element item : itemElements(xml)) {
            toNews(textOf(item, "title"), textOf(item, "link"), textOf(item, "description"),
                    imageOf(item), publishedAt(textOf(item, "pubDate")))
                    .ifPresent(news -> unique.putIfAbsent(news.getNewsMeta().getSourceUrl(), news));
        }
        return List.copyOf(unique.values());
    }

    static List<News> fromSearch(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode news = JSON.readTree(json).path("news");
            if (!news.isArray()) {
                return List.of();
            }
            LinkedHashMap<String, News> unique = new LinkedHashMap<>();
            for (JsonNode item : news) {
                toNews(item.path("title").asText(""), item.path("link").asText(""),
                        item.path("publisher").asText(""), thumbnail(item), epoch(item))
                        .ifPresent(row -> unique.putIfAbsent(row.getNewsMeta().getSourceUrl(), row));
            }
            return List.copyOf(unique.values());
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static Optional<News> toNews(String title, String url, String body, String image, LocalDateTime published) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty() || url == null || !url.startsWith("http")) {
            return Optional.empty();
        }
        String content = body == null || body.isBlank() ? trimmed : stripTags(body);
        NewsMeta meta = NewsMeta.of(NewsProvider.YAHOO_FINANCE, published, url.trim());
        return Optional.of(News.createWithoutAI(meta, new Content(trimmed, content).withImageUrl(blankToNull(image))));
    }

    private static List<Element> itemElements(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList nodes = document.getElementsByTagName("item");
            List<Element> items = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i) instanceof Element element) {
                    items.add(element);
                }
            }
            return items;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static String textOf(Element item, String tag) {
        NodeList nodes = item.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent();
    }

    private static String imageOf(Element item) {
        String found = imageUrl(item);
        return found == null ? "" : found;
    }

    private static String imageUrl(Node node) {
        if (node instanceof Element element) {
            String url = element.getAttribute("url");
            if (url.startsWith("http") && looksLikeImage(element, url)) {
                return url;
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            String found = imageUrl(children.item(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean looksLikeImage(Element element, String url) {
        String name = element.getLocalName() == null ? element.getTagName() : element.getLocalName();
        return "content".equals(name) || "thumbnail".equals(name) || "enclosure".equals(name)
                || url.contains(".jpg") || url.contains(".png") || url.contains(".webp");
    }

    private static LocalDateTime publishedAt(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return ZonedDateTime.parse(raw.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            return LocalDateTime.now();
        }
    }

    private static LocalDateTime epoch(JsonNode item) {
        long seconds = item.path("providerPublishTime").asLong(0);
        if (seconds <= 0) {
            return LocalDateTime.now();
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }

    private static String thumbnail(JsonNode item) {
        JsonNode resolutions = item.path("thumbnail").path("resolutions");
        if (!resolutions.isArray() || resolutions.isEmpty()) {
            return "";
        }
        return resolutions.get(resolutions.size() - 1).path("url").asText("");
    }

    private static String stripTags(String value) {
        return value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
