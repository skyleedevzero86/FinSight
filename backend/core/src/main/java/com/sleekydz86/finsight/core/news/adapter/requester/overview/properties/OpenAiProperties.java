package com.sleekydz86.finsight.core.news.adapter.requester.overview.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.openai.api")
public class OpenAiProperties {

    private String baseUrl = "https://api.openai.com/v1/chat/completions";
    private String apiKey = "";
    private String model = "gpt-3.5-turbo";

    public OpenAiProperties() {
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            this.baseUrl = "https://api.openai.com/v1/chat/completions";
            return;
        }
        this.baseUrl = baseUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public void setModel(String model) {
        if (model == null || model.isBlank()) {
            this.model = "gpt-3.5-turbo";
            return;
        }
        this.model = model;
    }

    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.contains("placeholder")
                && !apiKey.startsWith("local-");
    }

    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }
}
