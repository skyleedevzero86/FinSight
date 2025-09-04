package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.global.NewsProvider;

import java.time.LocalDateTime;

public class NewsMeta {

    private NewsProvider newsProvider;
    private LocalDateTime newsPublishedTime;
    private String sourceUrl;

    public NewsMeta() {
    }

    public NewsMeta(NewsProvider newsProvider, LocalDateTime newsPublishedTime, String sourceUrl) {
        this.newsProvider = newsProvider;
        this.newsPublishedTime = newsPublishedTime;
        this.sourceUrl = sourceUrl;
    }

    public static NewsMeta of(NewsProvider newsProvider, LocalDateTime newsPublishedTime, String sourceUrl) {
        return new NewsMeta(newsProvider, newsPublishedTime, sourceUrl);
    }

    public static NewsMeta of(NewsProvider newsProvider, String sourceUrl) {
        return new NewsMeta(newsProvider, LocalDateTime.now(), sourceUrl);
    }

    public NewsProvider getNewsProvider() {
        return newsProvider;
    }

    public void setNewsProvider(NewsProvider newsProvider) {
        this.newsProvider = newsProvider;
    }

    public LocalDateTime getNewsPublishedTime() {
        return newsPublishedTime;
    }

    public void setNewsPublishedTime(LocalDateTime newsPublishedTime) {
        this.newsPublishedTime = newsPublishedTime;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public boolean isValidProvider() {
        return this.newsProvider != null &&
                this.newsProvider != NewsProvider.ALL;
    }

    public boolean isValidPublishedTime() {
        return this.newsPublishedTime != null &&
                this.newsPublishedTime.isBefore(LocalDateTime.now());
    }

    public boolean isValidSourceUrl() {
        return this.sourceUrl != null &&
                !this.sourceUrl.trim().isEmpty() &&
                (this.sourceUrl.startsWith("http://") ||
                        this.sourceUrl.startsWith("https://"));
    }

    public boolean isValid() {
        return isValidProvider() &&
                isValidPublishedTime() &&
                isValidSourceUrl();
    }

    public boolean isPublishedWithin24Hours() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        return this.newsPublishedTime.isAfter(twentyFourHoursAgo);
    }

    public boolean isPublishedWithin7Days() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        return this.newsPublishedTime.isAfter(sevenDaysAgo);
    }

    public boolean isPublishedWithin30Days() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return this.newsPublishedTime.isAfter(thirtyDaysAgo);
    }

    public boolean isPublishedAfter(LocalDateTime time) {
        return this.newsPublishedTime != null &&
                this.newsPublishedTime.isAfter(time);
    }

    public boolean isPublishedBefore(LocalDateTime time) {
        return this.newsPublishedTime != null &&
                this.newsPublishedTime.isBefore(time);
    }

    public boolean isPublishedBetween(LocalDateTime start, LocalDateTime end) {
        return this.newsPublishedTime != null &&
                this.newsPublishedTime.isAfter(start) &&
                this.newsPublishedTime.isBefore(end);
    }

    public long getAgeInDays() {
        if (this.newsPublishedTime == null) {
            return -1;
        }
        return java.time.Duration.between(this.newsPublishedTime, LocalDateTime.now()).toDays();
    }

    public long getAgeInHours() {
        if (this.newsPublishedTime == null) {
            return -1;
        }
        return java.time.Duration.between(this.newsPublishedTime, LocalDateTime.now()).toHours();
    }

    public long getAgeInMinutes() {
        if (this.newsPublishedTime == null) {
            return -1;
        }
        return java.time.Duration.between(this.newsPublishedTime, LocalDateTime.now()).toMinutes();
    }

    public boolean isPublishedToday() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        return this.newsPublishedTime.isAfter(today) &&
                this.newsPublishedTime.isBefore(tomorrow);
    }

    public boolean isPublishedYesterday() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime yesterday = LocalDateTime.now().toLocalDate().minusDays(1).atStartOfDay();
        LocalDateTime today = yesterday.plusDays(1);
        return this.newsPublishedTime.isAfter(yesterday) &&
                this.newsPublishedTime.isBefore(today);
    }

    public boolean isPublishedThisWeek() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime startOfWeek = LocalDateTime.now()
                .toLocalDate()
                .with(java.time.DayOfWeek.MONDAY)
                .atStartOfDay();
        return this.newsPublishedTime.isAfter(startOfWeek);
    }

    public boolean isPublishedThisMonth() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime startOfMonth = LocalDateTime.now()
                .toLocalDate()
                .withDayOfMonth(1)
                .atStartOfDay();
        return this.newsPublishedTime.isAfter(startOfMonth);
    }

    public boolean isPublishedThisYear() {
        if (this.newsPublishedTime == null) {
            return false;
        }
        LocalDateTime startOfYear = LocalDateTime.now()
                .toLocalDate()
                .withDayOfYear(1)
                .atStartOfDay();
        return this.newsPublishedTime.isAfter(startOfYear);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NewsMeta newsMeta = (NewsMeta) o;
        return newsProvider == newsMeta.newsProvider &&
                java.util.Objects.equals(newsPublishedTime, newsMeta.newsPublishedTime) &&
                java.util.Objects.equals(sourceUrl, newsMeta.sourceUrl);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(newsProvider, newsPublishedTime, sourceUrl);
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