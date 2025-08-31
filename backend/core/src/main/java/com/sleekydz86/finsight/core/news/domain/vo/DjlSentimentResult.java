package com.sleekydz86.finsight.core.news.domain.vo;

import java.time.LocalDateTime;
import java.util.Objects;

public class DjlSentimentResult {

    private String label;
    private double score;
    private double confidence;
    private boolean success;
    private String errorMessage;
    private long processingTimeMs;
    private LocalDateTime analyzedAt;
    private String modelName;
    private String originalText;

    public DjlSentimentResult() {
        this.analyzedAt = LocalDateTime.now();
    }

    public static DjlSentimentResultBuilder builder() {
        return new DjlSentimentResultBuilder();
    }

    public static class DjlSentimentResultBuilder {
        private final DjlSentimentResult result = new DjlSentimentResult();

        public DjlSentimentResultBuilder label(String label) {
            result.label = label;
            return this;
        }

        public DjlSentimentResultBuilder score(double score) {
            result.score = score;
            return this;
        }

        public DjlSentimentResultBuilder confidence(double confidence) {
            result.confidence = confidence;
            return this;
        }

        public DjlSentimentResultBuilder success(boolean success) {
            result.success = success;
            return this;
        }

        public DjlSentimentResultBuilder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }

        public DjlSentimentResultBuilder processingTimeMs(long processingTimeMs) {
            result.processingTimeMs = processingTimeMs;
            return this;
        }

        public DjlSentimentResultBuilder modelName(String modelName) {
            result.modelName = modelName;
            return this;
        }

        public DjlSentimentResultBuilder originalText(String originalText) {
            result.originalText = originalText;
            return this;
        }

        public DjlSentimentResult build() {
            return result;
        }
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public SentimentType toSentimentType() {
        if (!success || label == null) {
            return SentimentType.NEUTRAL;
        }

        String upperLabel = label.toUpperCase();
        switch (upperLabel) {
            case "POSITIVE":
            case "POS":
                return SentimentType.POSITIVE;
            case "NEGATIVE":
            case "NEG":
                return SentimentType.NEGATIVE;
            case "NEUTRAL":
            case "NEU":
            default:
                return SentimentType.NEUTRAL;
        }
    }

    @Override
    public String toString() {
        return "DjlSentimentResult{" +
                "label='" + label + '\'' +
                ", score=" + score +
                ", confidence=" + confidence +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                ", analyzedAt=" + analyzedAt +
                ", modelName='" + modelName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DjlSentimentResult that = (DjlSentimentResult) o;
        return Double.compare(that.score, score) == 0 &&
                Double.compare(that.confidence, confidence) == 0 &&
                success == that.success &&
                processingTimeMs == that.processingTimeMs &&
                Objects.equals(label, that.label) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(analyzedAt, that.analyzedAt) &&
                Objects.equals(modelName, that.modelName) &&
                Objects.equals(originalText, that.originalText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, score, confidence, success, errorMessage, processingTimeMs, analyzedAt, modelName, originalText);
    }
}