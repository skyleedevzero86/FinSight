package com.sleekydz86.finsight.core.news.domain;

import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class News {
    private final Long id;
    private final NewsProvider newsProvider;
    private final LocalDateTime scrapedTime;
    private final Content originalContent;
    private final Content translatedContent;
    private final AiOverview aiOverView;

    public News(Long id, NewsProvider newsProvider, LocalDateTime scrapedTime,
                Content originalContent, Content translatedContent, AiOverview aiOverView) {
        this.id = id;
        this.newsProvider = newsProvider;
        this.scrapedTime = scrapedTime != null ? scrapedTime : LocalDateTime.now();
        this.originalContent = originalContent;
        this.translatedContent = translatedContent;
        this.aiOverView = aiOverView;
    }

    public boolean isContainsNewsProviderAndCategories(List<TargetCategory> categories,
                                                       List<NewsProvider> providers) {
        return providers.contains(this.newsProvider) &&
                this.aiOverView.isMatchedCategory(categories);
    }

    public Long getId() { return id; }
    public NewsProvider getNewsProvider() { return newsProvider; }
    public LocalDateTime getScrapedTime() { return scrapedTime; }
    public Content getOriginalContent() { return originalContent; }
    public Content getTranslatedContent() { return translatedContent; }
    public AiOverview getAiOverView() { return aiOverView; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        News news = (News) o;
        return Objects.equals(id, news.id) &&
                newsProvider == news.newsProvider &&
                Objects.equals(scrapedTime, news.scrapedTime) &&
                Objects.equals(originalContent, news.originalContent) &&
                Objects.equals(translatedContent, news.translatedContent) &&
                Objects.equals(aiOverView, news.aiOverView);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, newsProvider, scrapedTime, originalContent, translatedContent, aiOverView);
    }

    @Override
    public String toString() {
        return "News{" +
                "id=" + id +
                ", newsProvider=" + newsProvider +
                ", scrapedTime=" + scrapedTime +
                ", originalContent=" + originalContent +
                ", translatedContent=" + translatedContent +
                ", aiOverView=" + aiOverView +
                '}';
    }
}