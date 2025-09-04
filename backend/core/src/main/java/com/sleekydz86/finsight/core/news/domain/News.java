package com.sleekydz86.finsight.core.news.domain;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.*;

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
        private final NewsMeta newsMeta;

        public News() {
                this.id = null;
                this.newsProvider = null;
                this.scrapedTime = null;
                this.originalContent = null;
                this.translatedContent = null;
                this.aiOverView = null;
                this.newsMeta = null;
        }

        public News(Long id, NewsProvider newsProvider, LocalDateTime scrapedTime,
                        Content originalContent, Content translatedContent, AiOverview aiOverView, NewsMeta newsMeta) {
                this.id = id;
                this.newsProvider = newsProvider;
                this.scrapedTime = scrapedTime;
                this.originalContent = originalContent;
                this.translatedContent = translatedContent;
                this.aiOverView = aiOverView;
                this.newsMeta = newsMeta;
        }

        public News(Long id, NewsMeta newsMeta, LocalDateTime scrapedTime,
                        Content originalContent, Content translatedContent, AiOverview aiOverView) {

                this.id = (id != null && id != 0L) ? id : 0L;
                this.newsMeta = newsMeta;
                this.newsProvider = newsMeta != null ? newsMeta.getNewsProvider() : null;
                this.scrapedTime = scrapedTime != null ? scrapedTime : LocalDateTime.now();
                this.originalContent = originalContent;
                this.translatedContent = translatedContent;
                this.aiOverView = aiOverView;
        }

        public boolean isContainsNewsProviderAndCategories(List<TargetCategory> categories,
                        List<NewsProvider> providers) {

                boolean isMatchedCategory = (this.aiOverView != null)
                                ? this.aiOverView.isMatchedCategory(categories)
                                : false;

                return providers.contains(this.newsMeta.getNewsProvider()) && isMatchedCategory;
        }

        public Long getId() {
                return id;
        }

        public NewsProvider getNewsProvider() {
                return newsProvider;
        }

        public NewsMeta getNewsMeta() {
                return newsMeta;
        }

        public LocalDateTime getScrapedTime() {
                return scrapedTime;
        }

        public Content getOriginalContent() {
                return originalContent;
        }

        public Content getTranslatedContent() {
                return translatedContent;
        }

        public AiOverview getAiOverView() {
                return aiOverView;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o)
                        return true;
                if (o == null || getClass() != o.getClass())
                        return false;
                News news = (News) o;
                return Objects.equals(id, news.id) &&
                                newsProvider == news.newsProvider &&
                                Objects.equals(newsMeta, news.newsMeta) &&
                                Objects.equals(scrapedTime, news.scrapedTime) &&
                                Objects.equals(originalContent, news.originalContent) &&
                                Objects.equals(translatedContent, news.translatedContent) &&
                                Objects.equals(aiOverView, news.aiOverView);
        }

        public static News createWithoutAI(
                        NewsMeta newsMeta,
                        LocalDateTime scrapedTime,
                        Content originalContent) {
                return new News(
                                0L,
                                newsMeta,
                                scrapedTime,
                                originalContent,
                                null,
                                null);
        }

        public static News createWithoutAI(
                        NewsMeta newsMeta,
                        Content originalContent) {
                return createWithoutAI(newsMeta, LocalDateTime.now(), originalContent);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id, newsProvider, newsMeta, scrapedTime, originalContent, translatedContent,
                                aiOverView);
        }

        @Override
        public String toString() {
                return "News{" +
                                "id=" + id +
                                ", newsProvider=" + newsProvider +
                                ", newsMeta=" + newsMeta +
                                ", scrapedTime=" + scrapedTime +
                                ", originalContent=" + originalContent +
                                ", translatedContent=" + translatedContent +
                                ", aiOverView=" + aiOverView +
                                '}';
        }

        public News updateAiAnalysis(
                        String overview,
                        String translatedTitle,
                        String translatedContent,
                        List<TargetCategory> categories,
                        SentimentType sentimentType,
                        Double sentimentRatio) {

                Content newTranslatedContent = new Content(translatedTitle, translatedContent);

                AiOverview newAiOverView = new AiOverview(
                                overview,
                                sentimentType,
                                sentimentRatio,
                                categories);

                return new News(
                                this.id,
                                this.newsMeta,
                                this.scrapedTime,
                                this.originalContent,
                                newTranslatedContent,
                                newAiOverView);
        }

        public static Builder builder() {
                return new Builder();
        }

        public static class Builder {
                private Long id;
                private NewsProvider newsProvider;
                private LocalDateTime scrapedTime;
                private Content originalContent;
                private Content translatedContent;
                private AiOverview aiOverView;
                private NewsMeta newsMeta;

                public Builder id(Long id) {
                        this.id = id;
                        return this;
                }

                public Builder newsProvider(NewsProvider newsProvider) {
                        this.newsProvider = newsProvider;
                        return this;
                }

                public Builder scrapedTime(LocalDateTime scrapedTime) {
                        this.scrapedTime = scrapedTime;
                        return this;
                }

                public Builder originalContent(Content originalContent) {
                        this.originalContent = originalContent;
                        return this;
                }

                public Builder translatedContent(Content translatedContent) {
                        this.translatedContent = translatedContent;
                        return this;
                }

                public Builder aiOverView(AiOverview aiOverView) {
                        this.aiOverView = aiOverView;
                        return this;
                }

                public Builder newsMeta(NewsMeta newsMeta) {
                        this.newsMeta = newsMeta;
                        return this;
                }

                public News build() {
                        return new News(id, newsProvider, scrapedTime, originalContent, translatedContent, aiOverView,
                                        newsMeta);
                }
        }
}