package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.BaseEntity;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news")
public class NewsJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "news_provider", nullable = false)
    private NewsProvider newsProvider;

    @Column(name = "news_published_time", nullable = false)
    private LocalDateTime newsPublishedTime;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "scraped_time", nullable = false)
    private LocalDateTime scrapedTime;

    @Column(name = "original_title", nullable = false)
    private String originalTitle;

    @Lob
    @Column(name = "original_content", nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "ai_translated_title", columnDefinition = "TEXT")
    private String translatedTitle;

    @Lob
    @Column(name = "ai_translated_content", columnDefinition = "TEXT")
    private String translatedContent;

    @Lob
    @Column(name = "ai_overview", columnDefinition = "TEXT")
    private String overview;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_sentiment_type")
    private SentimentType sentimentType;

    @Column(name = "ai_sentiment_score")
    private Double sentimentScore;

    @ElementCollection(targetClass = TargetCategory.class)
    @CollectionTable(
            name = "news_target_categories",
            joinColumns = @JoinColumn(name = "news_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private List<TargetCategory> targetCategories = new ArrayList<>();

    public NewsJpaEntity() {
    }

    public NewsJpaEntity(Long id, NewsProvider newsProvider, LocalDateTime newsPublishedTime, String sourceUrl,
                         LocalDateTime scrapedTime, String originalTitle, String originalContent,
                         String translatedTitle, String translatedContent, String overview,
                         SentimentType sentimentType, Double sentimentScore, List<TargetCategory> targetCategories) {
        this.id = id;
        this.newsProvider = newsProvider;
        this.newsPublishedTime = newsPublishedTime;
        this.sourceUrl = sourceUrl;
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

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getScrapedTime() {
        return scrapedTime;
    }

    public void setScrapedTime(LocalDateTime scrapedTime) {
        this.scrapedTime = scrapedTime;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getTranslatedTitle() {
        return translatedTitle;
    }

    public void setTranslatedTitle(String translatedTitle) {
        this.translatedTitle = translatedTitle;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public SentimentType getSentimentType() {
        return sentimentType;
    }

    public void setSentimentType(SentimentType sentimentType) {
        this.sentimentType = sentimentType;
    }

    public Double getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public List<TargetCategory> getTargetCategories() {
        return targetCategories;
    }

    public void setTargetCategories(List<TargetCategory> targetCategories) {
        this.targetCategories = targetCategories != null ? targetCategories : new ArrayList<>();
    }
}