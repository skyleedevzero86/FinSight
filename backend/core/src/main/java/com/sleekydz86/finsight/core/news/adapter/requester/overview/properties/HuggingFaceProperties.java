package com.sleekydz86.finsight.core.news.adapter.requester.overview.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ai.huggingface")
public class HuggingFaceProperties {

    private Map<String, ModelInfo> models = new HashMap<>();
    private String defaultModel;
    private String token;

    public Map<String, ModelInfo> getModels() {
        return models;
    }

    public void setModels(Map<String, ModelInfo> models) {
        this.models = models;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public static class ModelInfo {
        private String name;
        private String url;
        private int timeout;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}