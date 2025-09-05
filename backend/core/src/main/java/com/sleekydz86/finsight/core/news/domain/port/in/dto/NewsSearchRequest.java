package com.sleekydz86.finsight.core.news.domain.port.in.dto;

import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.global.NewsProvider;
import java.time.LocalDateTime;
import java.util.List;

public class NewsSearchRequest {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private SentimentType sentimentType;
    private String keyword;
    private List<TargetCategory> categories;
    private List<NewsProvider> providers;
    private int page = 0;
    private int size = 20;

    public NewsSearchRequest() {
    }

    public NewsSearchRequest(LocalDateTime startDate, LocalDateTime endDate,
            SentimentType sentimentType, String keyword,
            List<TargetCategory> categories, List<NewsProvider> providers,
            int page, int size) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.sentimentType = sentimentType;
        this.keyword = keyword;
        this.categories = categories;
        this.providers = providers;
        this.page = page;
        this.size = size;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public SentimentType getSentimentType() {
        return sentimentType;
    }

    public void setSentimentType(SentimentType sentimentType) {
        this.sentimentType = sentimentType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<TargetCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<TargetCategory> categories) {
        this.categories = categories;
    }

    public List<NewsProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<NewsProvider> providers) {
        this.providers = providers;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}