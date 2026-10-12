package com.sleekydz86.finsight.core.economypick.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TrumpFeedClient {

    private static final Logger log = LoggerFactory.getLogger(TrumpFeedClient.class);
    private static final Pattern ITEM = Pattern.compile("<item>([\\s\\S]*?)</item>");
    private static final Pattern TITLE = Pattern.compile("<title[^>]*>(?:<!\\[CDATA\\[)?([\\s\\S]*?)(?:]]>)?</title>");
    private static final Pattern LINK = Pattern.compile("<link>([\\s\\S]*?)</link>");
    private static final String[] QUERIES = {
            "Trump+tariff+trade+policy",
            "Trump+Truth+Social",
            "Trump+executive+order+economy"
    };

    private final WebClient webClient;

    public TrumpFeedClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<Headline> headlines() {
        List<Headline> rows = new ArrayList<>();
        for (String query : QUERIES) {
            rows.addAll(fetch(query));
            if (rows.size() >= 24) {
                break;
            }
        }
        return rows.size() <= 24 ? rows : List.copyOf(rows.subList(0, 24));
    }

    private List<Headline> fetch(String query) {
        String url = "https://news.google.com/rss/search?q=" + query + "&hl=en-US&gl=US&ceid=US:en";
        try {
            String body = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "application/rss+xml, application/xml, text/xml")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(6));
            return parse(body);
        } catch (RuntimeException exception) {
            log.warn("트럼프 뉴스 피드를 가져오지 못했습니다. query={}", query, exception);
            return List.of();
        }
    }

    List<Headline> parse(String xml) {
        List<Headline> rows = new ArrayList<>();
        if (xml == null || xml.isBlank()) {
            return rows;
        }
        Matcher matcher = ITEM.matcher(xml);
        while (matcher.find() && rows.size() < 8) {
            String block = matcher.group(1);
            String title = clean(group(TITLE, block));
            if (!title.isBlank()) {
                rows.add(new Headline(title, "Google News", clean(group(LINK, block))));
            }
        }
        return rows;
    }

    private String group(Pattern pattern, String block) {
        Matcher matcher = pattern.matcher(block);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String clean(String value) {
        return value.replace("<![CDATA[", "")
                .replace("]]>", "")
                .replaceAll("<[^>]+>", " ")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record Headline(String title, String source, String link) {
    }
}
