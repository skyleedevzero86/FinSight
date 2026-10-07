package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OllamaProperties;
import com.sleekydz86.finsight.core.news.adapter.requester.overview.properties.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class NewsAiAvailability {

    static final String ADMIN_NOTICE = "관리자에게 문의주세요.";

    private static final Logger log = LoggerFactory.getLogger(NewsAiAvailability.class);

    private final OpenAiProperties openAiProperties;
    private final OllamaProperties ollamaProperties;
    private final HttpClient httpClient;

    public NewsAiAvailability(OpenAiProperties openAiProperties, OllamaProperties ollamaProperties) {
        this(openAiProperties, ollamaProperties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build());
    }

    NewsAiAvailability(OpenAiProperties openAiProperties, OllamaProperties ollamaProperties, HttpClient httpClient) {
        this.openAiProperties = openAiProperties;
        this.ollamaProperties = ollamaProperties;
        this.httpClient = httpClient;
    }

    public Status status() {
        boolean available = decide(openAiProperties.isConfigured(), ollamaProperties.isEnabled(), ollamaReachable());
        if (!available) {
            log.warn("뉴스 AI를 사용할 수 없습니다. OpenAI 키 또는 Ollama 연결을 확인하세요.");
        }
        return new Status(available, available ? null : ADMIN_NOTICE);
    }

    static boolean decide(boolean openAiConfigured, boolean ollamaEnabled, boolean ollamaReachable) {
        if (openAiConfigured) {
            return true;
        }
        return ollamaEnabled && ollamaReachable;
    }

    private boolean ollamaReachable() {
        if (!ollamaProperties.isEnabled()) {
            return false;
        }
        String base = ollamaProperties.getBaseUrl();
        if (base == null || base.isBlank()) {
            return false;
        }
        String root = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(root + "/api/tags"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.warn("Ollama 연결 확인에 실패했습니다");
            return false;
        }
    }

    public record Status(boolean available, String message) {
    }
}
