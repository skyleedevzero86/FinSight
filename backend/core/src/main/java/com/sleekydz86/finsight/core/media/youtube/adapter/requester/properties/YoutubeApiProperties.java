package com.sleekydz86.finsight.core.media.youtube.adapter.requester.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "youtube.api")
public class YoutubeApiProperties {

    private String baseUrl = "https://www.googleapis.com/youtube/v3";
    private String apiKey = "";
    private int maxResultsPerSource = 25;

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

    public int getMaxResultsPerSource() {
        return maxResultsPerSource;
    }

    public void setMaxResultsPerSource(int maxResultsPerSource) {
        this.maxResultsPerSource = maxResultsPerSource;
    }
}
