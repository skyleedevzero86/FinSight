package com.sleekydz86.finsight.core.news.domain.port.out.requester.dto;

import java.util.List;
import java.util.Objects;

public class AiChatRequest {
    private final String prompt;
    private final List<NewsItemRequest> newsItems;

    public AiChatRequest(String prompt, List<NewsItemRequest> newsItems) {
        this.prompt = prompt;
        this.newsItems = newsItems;
    }

    public String getPrompt() {
        return prompt;
    }

    public List<NewsItemRequest> getNewsItems() {
        return newsItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiChatRequest that = (AiChatRequest) o;
        return Objects.equals(prompt, that.prompt) &&
                Objects.equals(newsItems, that.newsItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prompt, newsItems);
    }

    @Override
    public String toString() {
        return "AiChatRequest{" +
                "prompt='" + prompt + '\'' +
                ", newsItems=" + newsItems +
                '}';
    }

    public static class NewsItemRequest {
        private final String title;
        private final String content;

        public NewsItemRequest(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NewsItemRequest that = (NewsItemRequest) o;
            return Objects.equals(title, that.title) &&
                    Objects.equals(content, that.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, content);
        }

        @Override
        public String toString() {
            return "NewsItemRequest{" +
                    "title='" + title + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }
}