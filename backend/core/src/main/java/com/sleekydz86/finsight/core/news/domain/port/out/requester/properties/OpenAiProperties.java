package com.sleekydz86.finsight.core.news.domain.port.out.requester.properties;

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
            throw new IllegalArgumentException("open-ai base-url must not be blank");
        }
        this.baseUrl = baseUrl;
    }

    public void setApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("open-ai api-key must not be blank");
        }
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("open-ai model must not be blank");
        }
        this.model = model;
    }

    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
}