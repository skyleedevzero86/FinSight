package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.vo.EmbeddingStatus;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentStatus;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news")
public class NewsJpaEntity {

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

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_sentiment_status", length = 16)
    private SentimentStatus sentimentStatus;

    @Column(name = "ai_sentiment_confidence")
    private Double sentimentConfidence;

    @Column(name = "ai_sentiment_positive")
    private Double sentimentPositive;

    @Column(name = "ai_sentiment_neutral")
    private Double sentimentNeutral;

    @Column(name = "ai_sentiment_negative")
    private Double sentimentNegative;

    @Column(name = "ai_sentiment_model", length = 200)
    private String sentimentModel;

    @Column(name = "ai_sentiment_attempts", nullable = false)
    private int sentimentAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_embedding_status", length = 16)
    private EmbeddingStatus embeddingStatus;

    @Column(name = "ai_embedding_model", length = 200)
    private String embeddingModel;

    @Column(name = "ai_embedding_attempts", nullable = false)
    private int embeddingAttempts;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @BatchSize(size = 50)
    @ElementCollection(targetClass = TargetCategory.class)
    @CollectionTable(name = "news_target_categories", joinColumns = @JoinColumn(name = "news_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private List<TargetCategory> targetCategories = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public NewsJpaEntity() {
    }

    public NewsJpaEntity(Long id, NewsProvider newsProvider, LocalDateTime newsPublishedTime, String sourceUrl,
                         LocalDateTime scrapedTime, String originalTitle, String originalContent,
                         String translatedTitle, String translatedContent, String overview,
                         SentimentType sentimentType, Double sentimentScore, int viewCount, List<TargetCategory> targetCategories) {
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
        this.viewCount = viewCount;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public SentimentStatus getSentimentStatus() {
        return sentimentStatus;
    }

    public void setSentimentStatus(SentimentStatus sentimentStatus) {
        this.sentimentStatus = sentimentStatus;
    }

    public Double getSentimentConfidence() {
        return sentimentConfidence;
    }

    public void setSentimentConfidence(Double sentimentConfidence) {
        this.sentimentConfidence = sentimentConfidence;
    }

    public Double getSentimentPositive() {
        return sentimentPositive;
    }

    public void setSentimentPositive(Double sentimentPositive) {
        this.sentimentPositive = sentimentPositive;
    }

    public Double getSentimentNeutral() {
        return sentimentNeutral;
    }

    public void setSentimentNeutral(Double sentimentNeutral) {
        this.sentimentNeutral = sentimentNeutral;
    }

    public Double getSentimentNegative() {
        return sentimentNegative;
    }

    public void setSentimentNegative(Double sentimentNegative) {
        this.sentimentNegative = sentimentNegative;
    }

    public String getSentimentModel() {
        return sentimentModel;
    }

    public void setSentimentModel(String sentimentModel) {
        this.sentimentModel = sentimentModel;
    }

    public int getSentimentAttempts() {
        return sentimentAttempts;
    }

    public void setSentimentAttempts(int sentimentAttempts) {
        this.sentimentAttempts = sentimentAttempts;
    }

    public EmbeddingStatus getEmbeddingStatus() {
        return embeddingStatus;
    }

    public void setEmbeddingStatus(EmbeddingStatus embeddingStatus) {
        this.embeddingStatus = embeddingStatus;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getEmbeddingAttempts() {
        return embeddingAttempts;
    }

    public void setEmbeddingAttempts(int embeddingAttempts) {
        this.embeddingAttempts = embeddingAttempts;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public List<TargetCategory> getTargetCategories() {
        return targetCategories;
    }

    public void setTargetCategories(List<TargetCategory> targetCategories) {
        this.targetCategories = targetCategories != null ? targetCategories : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}