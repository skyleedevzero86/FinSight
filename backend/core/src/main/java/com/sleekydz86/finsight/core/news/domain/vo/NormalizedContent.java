package com.sleekydz86.finsight.core.news.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

public class NormalizedContent {

    private Content original;
    private Content translated;
    private String normalizedTitle;
    private String normalizedBody;
    private String contentHash;
    private String urlHash;
    private LocalDateTime normalizedAt;
    private int qualityScore;
    private List<String> warnings;
    private List<String> errors;
    private boolean isSuccessfullyNormalized;
    private long processingTimeMs;
    private String normalizationVersion = "1.0";

    public NormalizedContent() {
    }

    public NormalizedContent(Content original, Content translated, String normalizedTitle, String normalizedBody,
                             String contentHash, String urlHash, LocalDateTime normalizedAt, int qualityScore,
                             List<String> warnings, List<String> errors, boolean isSuccessfullyNormalized,
                             long processingTimeMs, String normalizationVersion) {
        this.original = original;
        this.translated = translated;
        this.normalizedTitle = normalizedTitle;
        this.normalizedBody = normalizedBody;
        this.contentHash = contentHash;
        this.urlHash = urlHash;
        this.normalizedAt = normalizedAt;
        this.qualityScore = qualityScore;
        this.warnings = warnings;
        this.errors = errors;
        this.isSuccessfullyNormalized = isSuccessfullyNormalized;
        this.processingTimeMs = processingTimeMs;
        this.normalizationVersion = normalizationVersion;
    }

    public boolean isValid() {
        return normalizedTitle != null && !normalizedTitle.trim().isEmpty() &&
                normalizedBody != null && !normalizedBody.trim().isEmpty() &&
                qualityScore >= 0 && qualityScore <= 100;
    }

    public QualityGrade getQualityGrade() {
        if (qualityScore >= 90)
            return QualityGrade.EXCELLENT;
        if (qualityScore >= 80)
            return QualityGrade.GOOD;
        if (qualityScore >= 70)
            return QualityGrade.FAIR;
        if (qualityScore >= 60)
            return QualityGrade.POOR;
        return QualityGrade.UNACCEPTABLE;
    }

    public enum QualityGrade {
        EXCELLENT("우수", "90점 이상"),
        GOOD("양호", "80-89점"),
        FAIR("보통", "70-79점"),
        POOR("미흡", "60-69점"),
        UNACCEPTABLE("부적절", "60점 미만");

        private final String description;
        private final String scoreRange;

        QualityGrade(String description, String scoreRange) {
            this.description = description;
            this.scoreRange = scoreRange;
        }

        public String getDescription() {
            return description;
        }

        public String getScoreRange() {
            return scoreRange;
        }
    }

    public Content getOriginal() {
        return original;
    }

    public void setOriginal(Content original) {
        this.original = original;
    }

    public Content getTranslated() {
        return translated;
    }

    public void setTranslated(Content translated) {
        this.translated = translated;
    }

    public String getNormalizedTitle() {
        return normalizedTitle;
    }

    public void setNormalizedTitle(String normalizedTitle) {
        this.normalizedTitle = normalizedTitle;
    }

    public String getNormalizedBody() {
        return normalizedBody;
    }

    public void setNormalizedBody(String normalizedBody) {
        this.normalizedBody = normalizedBody;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getUrlHash() {
        return urlHash;
    }

    public void setUrlHash(String urlHash) {
        this.urlHash = urlHash;
    }

    public LocalDateTime getNormalizedAt() {
        return normalizedAt;
    }

    public void setNormalizedAt(LocalDateTime normalizedAt) {
        this.normalizedAt = normalizedAt;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public boolean isSuccessfullyNormalized() {
        return isSuccessfullyNormalized;
    }

    public void setSuccessfullyNormalized(boolean successfullyNormalized) {
        isSuccessfullyNormalized = successfullyNormalized;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getNormalizationVersion() {
        return normalizationVersion;
    }

    public void setNormalizationVersion(String normalizationVersion) {
        this.normalizationVersion = normalizationVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Content original;
        private Content translated;
        private String normalizedTitle;
        private String normalizedBody;
        private String contentHash;
        private String urlHash;
        private LocalDateTime normalizedAt;
        private int qualityScore;
        private List<String> warnings;
        private List<String> errors;
        private boolean isSuccessfullyNormalized;
        private long processingTimeMs;
        private String normalizationVersion = "1.0";

        public Builder original(Content original) {
            this.original = original;
            return this;
        }

        public Builder translated(Content translated) {
            this.translated = translated;
            return this;
        }

        public Builder normalizedTitle(String normalizedTitle) {
            this.normalizedTitle = normalizedTitle;
            return this;
        }

        public Builder normalizedBody(String normalizedBody) {
            this.normalizedBody = normalizedBody;
            return this;
        }

        public Builder contentHash(String contentHash) {
            this.contentHash = contentHash;
            return this;
        }

        public Builder urlHash(String urlHash) {
            this.urlHash = urlHash;
            return this;
        }

        public Builder normalizedAt(LocalDateTime normalizedAt) {
            this.normalizedAt = normalizedAt;
            return this;
        }

        public Builder qualityScore(int qualityScore) {
            this.qualityScore = qualityScore;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public Builder isSuccessfullyNormalized(boolean isSuccessfullyNormalized) {
            this.isSuccessfullyNormalized = isSuccessfullyNormalized;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder normalizationVersion(String normalizationVersion) {
            this.normalizationVersion = normalizationVersion;
            return this;
        }

        public NormalizedContent build() {
            return new NormalizedContent(original, translated, normalizedTitle, normalizedBody, contentHash, urlHash,
                    normalizedAt, qualityScore, warnings, errors, isSuccessfullyNormalized, processingTimeMs,
                    normalizationVersion);
        }
    }
}