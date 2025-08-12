package com.sleekydz86.finsight.core.news.domain.port.in;

import com.sleekydz86.finsight.core.news.domain.vo.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class NewsQueryRequest {
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final SentimentType sentimentType;
    private final List<NewsProvider> newsProviders;
    private final List<TargetCategory> categories;

    public NewsQueryRequest(LocalDateTime startDate, LocalDateTime endDate, SentimentType sentimentType,
                            List<NewsProvider> newsProviders, List<TargetCategory> categories) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.sentimentType = sentimentType;
        this.newsProviders = newsProviders;
        this.categories = categories;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public SentimentType getSentimentType() {
        return sentimentType;
    }

    public List<NewsProvider> getNewsProviders() {
        return newsProviders;
    }

    public List<TargetCategory> getCategories() {
        return categories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsQueryRequest that = (NewsQueryRequest) o;
        return Objects.equals(startDate, that.startDate) &&
                Objects.equals(endDate, that.endDate) &&
                sentimentType == that.sentimentType &&
                Objects.equals(newsProviders, that.newsProviders) &&
                Objects.equals(categories, that.categories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate, sentimentType, newsProviders, categories);
    }

    @Override
    public String toString() {
        return "NewsQueryRequest{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", sentimentType=" + sentimentType +
                ", newsProviders=" + newsProviders +
                ", categories=" + categories +
                '}';
    }
}