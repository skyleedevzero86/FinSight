package com.sleekydz86.finsight.core.news.domain.port.in.dto;

import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.NewsProvider;
import java.time.LocalDateTime;
import java.util.List;

public class NewsQueryRequest {
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final SentimentType sentimentType;
    private final String keyword;
    private final List<TargetCategory> categories;
    private final List<NewsProvider> providers;

    public NewsQueryRequest(LocalDateTime startDate, LocalDateTime endDate,
            SentimentType sentimentType, String keyword,
            List<TargetCategory> categories, List<NewsProvider> providers) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.sentimentType = sentimentType;
        this.keyword = keyword;
        this.categories = categories;
        this.providers = providers;
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

    public String getKeyword() {
        return keyword;
    }

    public List<TargetCategory> getCategories() {
        return categories;
    }

    public List<NewsProvider> getProviders() {
        return providers;
    }
}