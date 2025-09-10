package com.sleekydz86.finsight.core.news.domain.vo;

import java.util.List;
import java.util.Objects;

public class AiOverview {
    private final String overview;
    private final SentimentType sentimentType;
    private final double sentimentScore;
    private final List<TargetCategory> targetCategories;
    private final ImpactLevel impactLevel;
    private final double confidenceScore;

    public AiOverview(String overview, SentimentType sentimentType,
                      double sentimentScore, List<TargetCategory> targetCategories) {
        this(overview, sentimentType, sentimentScore, targetCategories,
                ImpactLevel.MEDIUM, 0.5);
    }

    public AiOverview(String overview, SentimentType sentimentType,
                      double sentimentScore, List<TargetCategory> targetCategories,
                      ImpactLevel impactLevel, double confidenceScore) {
        this.overview = overview;
        this.sentimentType = sentimentType;
        this.sentimentScore = sentimentScore;
        this.targetCategories = targetCategories;
        this.impactLevel = impactLevel != null ? impactLevel : ImpactLevel.MEDIUM;
        this.confidenceScore = confidenceScore;
    }

    public boolean isMatchedCategory(List<TargetCategory> givenCategories) {
        return targetCategories.stream()
                .anyMatch(givenCategories::contains);
    }

    public AiOverview copy(String overview, SentimentType sentimentType,
                           double sentimentScore, List<TargetCategory> targetCategories) {
        return new AiOverview(
                overview != null ? overview : this.overview,
                sentimentType != null ? sentimentType : this.sentimentType,
                sentimentScore != 0.0 ? sentimentScore : this.sentimentScore,
                targetCategories != null ? targetCategories : this.targetCategories,
                this.impactLevel,
                this.confidenceScore
        );
    }

    public AiOverview copyWithTargetCategories(List<TargetCategory> targetCategories) {
        return new AiOverview(this.overview, this.sentimentType, this.sentimentScore,
                targetCategories, this.impactLevel, this.confidenceScore);
    }

    public String getOverview() { return overview; }
    public String getSummary() { return overview; }
    public SentimentType getSentimentType() { return sentimentType; }
    public double getSentimentScore() { return sentimentScore; }
    public List<TargetCategory> getTargetCategories() { return targetCategories; }
    public ImpactLevel getImpactLevel() { return impactLevel; }
    public double getConfidenceScore() { return confidenceScore; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiOverview that = (AiOverview) o;
        return Double.compare(that.sentimentScore, sentimentScore) == 0 &&
                Double.compare(that.confidenceScore, confidenceScore) == 0 &&
                Objects.equals(overview, that.overview) &&
                sentimentType == that.sentimentType &&
                Objects.equals(targetCategories, that.targetCategories) &&
                impactLevel == that.impactLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(overview, sentimentType, sentimentScore,
                targetCategories, impactLevel, confidenceScore);
    }

    @Override
    public String toString() {
        return "AiOverview{" +
                "overview='" + overview + '\'' +
                ", sentimentType=" + sentimentType +
                ", sentimentScore=" + sentimentScore +
                ", targetCategories=" + targetCategories +
                ", impactLevel=" + impactLevel +
                ", confidenceScore=" + confidenceScore +
                '}';
    }

    public enum ImpactLevel {
        LOW("낮음"),
        MEDIUM("보통"),
        HIGH("높음"),
        CRITICAL("매우 높음");

        private final String description;

        ImpactLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}