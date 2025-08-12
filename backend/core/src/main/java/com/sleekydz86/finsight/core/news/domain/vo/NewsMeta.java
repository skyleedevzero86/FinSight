package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.global.NewsProvider;

import java.time.LocalDateTime;
import java.util.Objects;

public class NewsMeta {

    private final NewsProvider newsProvider;
    private final LocalDateTime newsPublishedTime;
    private final String sourceUrl;

    public NewsMeta(NewsProvider newsProvider, LocalDateTime newsPublishedTime, String sourceUrl) {
        this.newsProvider = newsProvider;
        this.newsPublishedTime = newsPublishedTime;
        this.sourceUrl = sourceUrl;
    }

    public static NewsMeta of(NewsProvider newsProvider, String newsPublishedTime, String sourceUrl) {
        return new NewsMeta(
                newsProvider,
                LocalDateTime.parse(newsPublishedTime),
                sourceUrl
        );
    }

    public NewsProvider getNewsProvider() {
        return newsProvider;
    }

    public LocalDateTime getNewsPublishedTime() {
        return newsPublishedTime;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NewsMeta newsMeta = (NewsMeta) obj;
        return Objects.equals(newsProvider, newsMeta.newsProvider) &&
                Objects.equals(newsPublishedTime, newsMeta.newsPublishedTime) &&
                Objects.equals(sourceUrl, newsMeta.sourceUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(newsProvider, newsPublishedTime, sourceUrl);
    }

    @Override
    public String toString() {
        return "NewsMeta{" +
                "newsProvider=" + newsProvider +
                ", newsPublishedTime=" + newsPublishedTime +
                ", sourceUrl='" + sourceUrl + '\'' +
                '}';
    }
}