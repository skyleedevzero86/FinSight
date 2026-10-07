package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "news.alphavantage.api")
public class AlphaVantageProperties {

    private String baseUrl;
    private String apiKey;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://www.alphavantage.co/query";
        }
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
