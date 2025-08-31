package com.sleekydz86.finsight.core.news.domain.vo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SentimentAnalysisResult {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final boolean success;
    private final SentimentType sentimentType;
    private final Double score;
    private final String errorMessage;
    private final LocalDateTime processedAt;
    private final long processingTimeMs;
    private final String sentimentDescription;

    public SentimentAnalysisResult(boolean success, SentimentType sentimentType, Double score,
                                   String errorMessage, LocalDateTime processedAt, long processingTimeMs,
                                   String sentimentDescription) {
        this.success = success;
        this.sentimentType = sentimentType;
        this.score = score;
        this.errorMessage = errorMessage;
        this.processedAt = processedAt;
        this.processingTimeMs = processingTimeMs;
        this.sentimentDescription = sentimentDescription;
    }

    public static SentimentAnalysisResult success(SentimentType sentimentType, Double score,
                                                  long processingTimeMs, String sentimentDescription) {
        if (sentimentType == null) {
            throw new IllegalArgumentException("감정 타입은 null일 수 없습니다.");
        }
        if (score == null || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("점수는 0.0과 1.0 사이의 값이어야 합니다.");
        }
        if (sentimentDescription == null || sentimentDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("감정 설명은 비어있을 수 없습니다.");
        }

        return new SentimentAnalysisResult(true, sentimentType, score, null,
                LocalDateTime.now(), processingTimeMs, sentimentDescription);
    }

    public static SentimentAnalysisResult failure(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("오류 메시지는 비어있을 수 없습니다.");
        }

        return new SentimentAnalysisResult(false, null, null, errorMessage,
                LocalDateTime.now(), 0, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public SentimentType getSentimentType() {
        return sentimentType;
    }

    public Double getScore() {
        return score;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public String getSentimentDescription() {
        return sentimentDescription;
    }

    public String getFormattedConfidence() {
        if (score == null)
            return "N/A";
        return String.format("%.1f%%", score * 100);
    }

    public String getFormattedProcessingTime() {
        if (processingTimeMs < 1000) {
            return processingTimeMs + "ms";
        } else {
            return String.format("%.2fs", processingTimeMs / 1000.0);
        }
    }

    public String getFormattedProcessedAt() {
        return processedAt != null ? processedAt.format(FORMATTER) : "N/A";
    }

    public String getSummary() {
        if (!success) {
            return "분석 실패: " + errorMessage;
        }

        return String.format("%s (신뢰도: %s, 처리시간: %s)",
                sentimentType, getFormattedConfidence(), getFormattedProcessingTime());
    }

    public boolean isValid() {
        if (!success) {
            return errorMessage != null && !errorMessage.trim().isEmpty();
        }

        return sentimentType != null &&
                score != null && score >= 0.0 && score <= 1.0 &&
                sentimentDescription != null && !sentimentDescription.trim().isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        SentimentAnalysisResult other = (SentimentAnalysisResult) obj;
        return success == other.success &&
                processingTimeMs == other.processingTimeMs &&
                java.util.Objects.equals(sentimentType, other.sentimentType) &&
                java.util.Objects.equals(score, other.score) &&
                java.util.Objects.equals(errorMessage, other.errorMessage) &&
                java.util.Objects.equals(processedAt, other.processedAt) &&
                java.util.Objects.equals(sentimentDescription, other.sentimentDescription);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(success, sentimentType, score, errorMessage,
                processedAt, processingTimeMs, sentimentDescription);
    }

    @Override
    public String toString() {
        if (!success) {
            return String.format("SentimentAnalysisResult{success=false, errorMessage='%s', processedAt=%s}",
                    errorMessage, getFormattedProcessedAt());
        }

        return String.format("SentimentAnalysisResult{success=true, sentimentType=%s, score=%.3f, " +
                        "processingTimeMs=%d, processedAt=%s, description='%s'}",
                sentimentType, score, processingTimeMs, getFormattedProcessedAt(),
                sentimentDescription != null && sentimentDescription.length() > 50
                        ? sentimentDescription.substring(0, 50) + "..."
                        : sentimentDescription);
    }
}