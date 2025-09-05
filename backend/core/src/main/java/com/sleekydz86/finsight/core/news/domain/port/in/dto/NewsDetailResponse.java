package com.sleekydz86.finsight.core.news.domain.port.in.dto;

import com.sleekydz86.finsight.core.comment.domain.Comment;
import com.sleekydz86.finsight.core.comment.domain.Comments;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.NewsStatistics;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import java.time.LocalDateTime;
import java.util.List;

public class NewsDetailResponse {
    private final Long id;
    private final NewsProvider newsProvider;
    private final String originalTitle;
    private final String originalContent;
    private final String translatedTitle;
    private final String translatedContent;
    private final String overview;
    private final SentimentType sentimentType;
    private final Double sentimentScore;
    private final List<TargetCategory> categories;
    private final LocalDateTime publishedTime;
    private final LocalDateTime scrapedTime;
    private final String sourceUrl;
    private final NewsStatistics statistics;
    private final Comments comments;
    private final List<News> relatedNews;

    public NewsDetailResponse() {
        this.id = null;
        this.newsProvider = null;
        this.originalTitle = null;
        this.originalContent = null;
        this.translatedTitle = null;
        this.translatedContent = null;
        this.overview = null;
        this.sentimentType = null;
        this.sentimentScore = null;
        this.categories = null;
        this.publishedTime = null;
        this.scrapedTime = null;
        this.sourceUrl = null;
        this.statistics = null;
        this.comments = new Comments();
        this.relatedNews = null;
    }

    public NewsDetailResponse(Long id, NewsProvider newsProvider, String originalTitle,
                              String originalContent, String translatedTitle, String translatedContent,
                              String overview, SentimentType sentimentType, Double sentimentScore,
                              List<TargetCategory> categories, LocalDateTime publishedTime,
                              LocalDateTime scrapedTime, String sourceUrl, NewsStatistics statistics,
                              Comments comments, List<News> relatedNews) {
        this.id = id;
        this.newsProvider = newsProvider;
        this.originalTitle = originalTitle;
        this.originalContent = originalContent;
        this.translatedTitle = translatedTitle;
        this.translatedContent = translatedContent;
        this.overview = overview;
        this.sentimentType = sentimentType;
        this.sentimentScore = sentimentScore;
        this.categories = categories;
        this.publishedTime = publishedTime;
        this.scrapedTime = scrapedTime;
        this.sourceUrl = sourceUrl;
        this.statistics = statistics;
        this.comments = comments;
        this.relatedNews = relatedNews;
    }

    public Long getId() { return id; }
    public NewsProvider getNewsProvider() { return newsProvider; }
    public String getOriginalTitle() { return originalTitle; }
    public String getOriginalContent() { return originalContent; }
    public String getTranslatedTitle() { return translatedTitle; }
    public String getTranslatedContent() { return translatedContent; }
    public String getOverview() { return overview; }
    public SentimentType getSentimentType() { return sentimentType; }
    public Double getSentimentScore() { return sentimentScore; }
    public List<TargetCategory> getCategories() { return categories; }
    public LocalDateTime getPublishedTime() { return publishedTime; }
    public LocalDateTime getScrapedTime() { return scrapedTime; }
    public String getSourceUrl() { return sourceUrl; }
    public NewsStatistics getStatistics() { return statistics; }
    public Comments getComments() { return comments; }
    public List<News> getRelatedNews() { return relatedNews; }

    @Override
    public String toString() {
        return "NewsDetailResponse{" +
                "id=" + id +
                ", newsProvider=" + newsProvider +
                ", originalTitle='" + originalTitle + '\'' +
                ", translatedTitle='" + translatedTitle + '\'' +
                ", overview='" + overview + '\'' +
                ", sentimentType=" + sentimentType +
                ", sentimentScore=" + sentimentScore +
                ", categories=" + categories +
                ", publishedTime=" + publishedTime +
                ", scrapedTime=" + scrapedTime +
                ", sourceUrl='" + sourceUrl + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private NewsProvider newsProvider;
        private String originalTitle;
        private String originalContent;
        private String translatedTitle;
        private String translatedContent;
        private String overview;
        private SentimentType sentimentType;
        private Double sentimentScore;
        private List<TargetCategory> categories;
        private LocalDateTime publishedTime;
        private LocalDateTime scrapedTime;
        private String sourceUrl;
        private NewsStatistics statistics;
        private Comments comments = new Comments();
        private List<News> relatedNews;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder newsProvider(NewsProvider newsProvider) {
            this.newsProvider = newsProvider;
            return this;
        }

        public Builder originalTitle(String originalTitle) {
            this.originalTitle = originalTitle;
            return this;
        }

        public Builder originalContent(String originalContent) {
            this.originalContent = originalContent;
            return this;
        }

        public Builder translatedTitle(String translatedTitle) {
            this.translatedTitle = translatedTitle;
            return this;
        }

        public Builder translatedContent(String translatedContent) {
            this.translatedContent = translatedContent;
            return this;
        }

        public Builder overview(String overview) {
            this.overview = overview;
            return this;
        }

        public Builder sentimentType(SentimentType sentimentType) {
            this.sentimentType = sentimentType;
            return this;
        }

        public Builder sentimentScore(Double sentimentScore) {
            this.sentimentScore = sentimentScore;
            return this;
        }

        public Builder categories(List<TargetCategory> categories) {
            this.categories = categories;
            return this;
        }

        public Builder publishedTime(LocalDateTime publishedTime) {
            this.publishedTime = publishedTime;
            return this;
        }

        public Builder scrapedTime(LocalDateTime scrapedTime) {
            this.scrapedTime = scrapedTime;
            return this;
        }

        public Builder sourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
            return this;
        }

        public Builder statistics(NewsStatistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Builder comments(Comments comments) {
            this.comments = comments;
            return this;
        }

        public Builder relatedNews(List<News> relatedNews) {
            this.relatedNews = relatedNews;
            return this;
        }

        public NewsDetailResponse build() {
            return new NewsDetailResponse(id, newsProvider, originalTitle, originalContent,
                    translatedTitle, translatedContent, overview, sentimentType,
                    sentimentScore, categories, publishedTime, scrapedTime,
                    sourceUrl, statistics, comments, relatedNews);
        }
    }
}
