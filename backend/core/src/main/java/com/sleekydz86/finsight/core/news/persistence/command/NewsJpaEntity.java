package com.sleekydz86.finsight.core.news.persistence.command;

import com.sleekydz86.finsight.core.global.BaseEntity;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "news")
public class NewsJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "news_provider", nullable = false)
    private final NewsProvider newsProvider;

    @Column(name = "scraped_time", nullable = false)
    private final LocalDateTime scrapedTime;

    @Column(name = "original_title", nullable = false)
    private final String originalTitle;

    @Lob
    @Column(name = "original_content", nullable = false)
    private final String originalContent;

    @Column(name = "ai_translated_title")
    private final String translatedTitle;

    @Lob
    @Column(name = "ai_translated_content")
    private final String translatedContent;

    @Lob
    @Column(name = "ai_overview")
    private final String overview;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_sentiment_type")
    private final SentimentType sentimentType;

    @Column(name = "ai_sentiment_score")
    private final Double sentimentScore;

    @ElementCollection(targetClass = TargetCategory.class)
    @CollectionTable(
            name = "news_target_categories",
            joinColumns = @JoinColumn(name = "news_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private final List<TargetCategory> targetCategories;

    protected NewsJpaEntity() {
        this.id = null;
        this.newsProvider = null;
        this.scrapedTime = null;
        this.originalTitle = null;
        this.originalContent = null;
        this.translatedTitle = null;
        this.translatedContent = null;
        this.overview = null;
        this.sentimentType = null;
        this.sentimentScore = null;
        this.targetCategories = new ArrayList<>();
    }

    public NewsJpaEntity(Long id, NewsProvider newsProvider, LocalDateTime scrapedTime,
                         String originalTitle, String originalContent, String translatedTitle,
                         String translatedContent, String overview, SentimentType sentimentType,
                         Double sentimentScore, List<TargetCategory> targetCategories) {
        this.id = id;
        this.newsProvider = newsProvider;
        this.scrapedTime = scrapedTime;
        this.originalTitle = originalTitle;
        this.originalContent = originalContent;
        this.translatedTitle = translatedTitle;
        this.translatedContent = translatedContent;
        this.overview = overview;
        this.sentimentType = sentimentType;
        this.sentimentScore = sentimentScore;
        this.targetCategories = targetCategories != null ? targetCategories : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public NewsProvider getNewsProvider() {
        return newsProvider;
    }

    public LocalDateTime getScrapedTime() {
        return scrapedTime;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public String getOverview() {
        return overview;
    }

    public SentimentType getSentimentType() {
        return sentimentType;
    }

    public Double getSentimentScore() {
        return sentimentScore;
    }

    public List<TargetCategory> getTargetCategories() {
        return targetCategories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsJpaEntity that = (NewsJpaEntity) o;
        return Objects.equals(id, that.id) &&
                newsProvider == that.newsProvider &&
                Objects.equals(scrapedTime, that.scrapedTime) &&
                Objects.equals(originalTitle, that.originalTitle) &&
                Objects.equals(originalContent, that.originalContent) &&
                Objects.equals(translatedTitle, that.translatedTitle) &&
                Objects.equals(translatedContent, that.translatedContent) &&
                Objects.equals(overview, that.overview) &&
                sentimentType == that.sentimentType &&
                Objects.equals(sentimentScore, that.sentimentScore) &&
                Objects.equals(targetCategories, that.targetCategories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, newsProvider, scrapedTime, originalTitle, originalContent,
                translatedTitle, translatedContent, overview, sentimentType,
                sentimentScore, targetCategories);
    }

    @Override
    public String toString() {
        return "NewsJpaEntity{" +
                "id=" + id +
                ", newsProvider=" + newsProvider +
                ", scrapedTime=" + scrapedTime +
                ", originalTitle='" + originalTitle + '\'' +
                ", originalContent='" + originalContent + '\'' +
                ", translatedTitle='" + translatedTitle + '\'' +
                ", translatedContent='" + translatedContent + '\'' +
                ", overview='" + overview + '\'' +
                ", sentimentType=" + sentimentType +
                ", sentimentScore=" + sentimentScore +
                ", targetCategories=" + targetCategories +
                '}';
    }
}