package com.sleekydz86.finsight.core.news.adapter.requester.overview.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.openai.api")
public class OpenAiProperties {

    private String baseUrl;
    private String apiKey;
    private String model;

    public OpenAiProperties() {
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("OpenAI base-url은 비어 있을 수 없습니다");
        }
        this.baseUrl = baseUrl;
    }

    public void setApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OpenAI api-key는 비어 있을 수 없습니다");
        }
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("OpenAI model은 비어 있을 수 없습니다");
        }
        this.model = model;
    }

    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
}