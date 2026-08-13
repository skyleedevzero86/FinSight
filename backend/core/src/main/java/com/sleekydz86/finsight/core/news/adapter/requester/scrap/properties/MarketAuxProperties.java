package com.sleekydz86.finsight.core.news.adapter.requester.scrap.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "news.marketaux.api")
public class MarketAuxProperties {

    private String baseUrl;
    private String apiKey;

    public MarketAuxProperties() {
    }

    public MarketAuxProperties(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        validate();
    }

    private void validate() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("MarketAux base-url은 비어 있을 수 없습니다");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getBaseUrl() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketAuxProperties that = (MarketAuxProperties) o;
        return java.util.Objects.equals(baseUrl, that.baseUrl) &&
                java.util.Objects.equals(apiKey, that.apiKey);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(baseUrl, apiKey);
    }

    @Override
    public String toString() {
        return "MarketAuxProperties{" +
                "baseUrl='" + baseUrl + '\'' +
                ", apiKey='" + apiKey + '\'' +
                '}';
    }
}