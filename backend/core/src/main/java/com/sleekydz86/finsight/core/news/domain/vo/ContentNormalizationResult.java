package com.sleekydz86.finsight.core.news.domain.vo;

import com.sleekydz86.finsight.core.news.domain.News;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ContentNormalizationResult {

    private News originalNews;
    private NormalizedContent normalizedContent;
    private boolean success;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private long processingTimeMs;
    private Map<String, Long> stepProcessingTimes;
    private List<NormalizationWarning> warnings;
    private List<NormalizationError> errors;
    private NormalizationStatistics statistics;
    private NormalizationMetadata metadata;

    public ContentNormalizationResult() {
    }

    public ContentNormalizationResult(News originalNews, NormalizedContent normalizedContent, boolean success,
                                      LocalDateTime startedAt, LocalDateTime completedAt, long processingTimeMs,
                                      Map<String, Long> stepProcessingTimes, List<NormalizationWarning> warnings,
                                      List<NormalizationError> errors, NormalizationStatistics statistics,
                                      NormalizationMetadata metadata) {
        this.originalNews = originalNews;
        this.normalizedContent = normalizedContent;
        this.success = success;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.processingTimeMs = processingTimeMs;
        this.stepProcessingTimes = stepProcessingTimes;
        this.warnings = warnings;
        this.errors = errors;
        this.statistics = statistics;
        this.metadata = metadata;
    }

    public boolean isSuccess() {
        return success && normalizedContent != null && normalizedContent.isValid();
    }

    public News getOriginalNews() {
        return originalNews;
    }

    public void setOriginalNews(News originalNews) {
        this.originalNews = originalNews;
    }

    public NormalizedContent getNormalizedContent() {
        return normalizedContent;
    }

    public void setNormalizedContent(NormalizedContent normalizedContent) {
        this.normalizedContent = normalizedContent;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Map<String, Long> getStepProcessingTimes() {
        return stepProcessingTimes;
    }

    public void setStepProcessingTimes(Map<String, Long> stepProcessingTimes) {
        this.stepProcessingTimes = stepProcessingTimes;
    }

    public List<NormalizationWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<NormalizationWarning> warnings) {
        this.warnings = warnings;
    }

    public List<NormalizationError> getErrors() {
        return errors;
    }

    public void setErrors(List<NormalizationError> errors) {
        this.errors = errors;
    }

    public NormalizationStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(NormalizationStatistics statistics) {
        this.statistics = statistics;
    }

    public NormalizationMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(NormalizationMetadata metadata) {
        this.metadata = metadata;
    }

    public static class NormalizationWarning {
        private String code;
        private String message;
        private String step;
        private LocalDateTime occurredAt;
        private WarningSeverity severity;

        public NormalizationWarning() {
        }

        public NormalizationWarning(String code, String message, String step, LocalDateTime occurredAt,
                                    WarningSeverity severity) {
            this.code = code;
            this.message = message;
            this.step = step;
            this.occurredAt = occurredAt;
            this.severity = severity;
        }

        public enum WarningSeverity {
            LOW, MEDIUM, HIGH, CRITICAL
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStep() {
            return step;
        }

        public void setStep(String step) {
            this.step = step;
        }

        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }

        public void setOccurredAt(LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
        }

        public WarningSeverity getSeverity() {
            return severity;
        }

        public void setSeverity(WarningSeverity severity) {
            this.severity = severity;
        }
    }

    public static class NormalizationError {
        private String code;
        private String message;
        private String step;
        private LocalDateTime occurredAt;
        private ErrorType type;
        private String stackTrace;

        public NormalizationError() {
        }

        public NormalizationError(String code, String message, String step, LocalDateTime occurredAt, ErrorType type,
                                  String stackTrace) {
            this.code = code;
            this.message = message;
            this.step = step;
            this.occurredAt = occurredAt;
            this.type = type;
            this.stackTrace = stackTrace;
        }

        public enum ErrorType {
            VALIDATION_ERROR, PROCESSING_ERROR, SYSTEM_ERROR, TIMEOUT_ERROR
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getStep() {
            return step;
        }

        public void setStep(String step) {
            this.step = step;
        }

        public LocalDateTime getOccurredAt() {
            return occurredAt;
        }

        public void setOccurredAt(LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
        }

        public ErrorType getType() {
            return type;
        }

        public void setType(ErrorType type) {
            this.type = type;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        public void setStackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
        }
    }

    public static class NormalizationStatistics {
        private int originalTitleLength;
        private int normalizedTitleLength;
        private int originalBodyLength;
        private int normalizedBodyLength;
        private int removedHtmlTags;
        private int removedControlCharacters;
        private int removedSpecialCharacters;
        private double compressionRatio;
        private int wordCount;
        private int sentenceCount;
        private int paragraphCount;

        public NormalizationStatistics() {
        }

        public NormalizationStatistics(int originalTitleLength, int normalizedTitleLength, int originalBodyLength,
                                       int normalizedBodyLength, int removedHtmlTags, int removedControlCharacters,
                                       int removedSpecialCharacters, double compressionRatio, int wordCount,
                                       int sentenceCount, int paragraphCount) {
            this.originalTitleLength = originalTitleLength;
            this.normalizedTitleLength = normalizedTitleLength;
            this.originalBodyLength = originalBodyLength;
            this.normalizedBodyLength = normalizedBodyLength;
            this.removedHtmlTags = removedHtmlTags;
            this.removedControlCharacters = removedControlCharacters;
            this.removedSpecialCharacters = removedSpecialCharacters;
            this.compressionRatio = compressionRatio;
            this.wordCount = wordCount;
            this.sentenceCount = sentenceCount;
            this.paragraphCount = paragraphCount;
        }

        public int getOriginalTitleLength() {
            return originalTitleLength;
        }

        public void setOriginalTitleLength(int originalTitleLength) {
            this.originalTitleLength = originalTitleLength;
        }

        public int getNormalizedTitleLength() {
            return normalizedTitleLength;
        }

        public void setNormalizedTitleLength(int normalizedTitleLength) {
            this.normalizedTitleLength = normalizedTitleLength;
        }

        public int getOriginalBodyLength() {
            return originalBodyLength;
        }

        public void setOriginalBodyLength(int originalBodyLength) {
            this.originalBodyLength = originalBodyLength;
        }

        public int getNormalizedBodyLength() {
            return normalizedBodyLength;
        }

        public void setNormalizedBodyLength(int normalizedBodyLength) {
            this.normalizedBodyLength = normalizedBodyLength;
        }

        public int getRemovedHtmlTags() {
            return removedHtmlTags;
        }

        public void setRemovedHtmlTags(int removedHtmlTags) {
            this.removedHtmlTags = removedHtmlTags;
        }

        public int getRemovedControlCharacters() {
            return removedControlCharacters;
        }

        public void setRemovedControlCharacters(int removedControlCharacters) {
            this.removedControlCharacters = removedControlCharacters;
        }

        public int getRemovedSpecialCharacters() {
            return removedSpecialCharacters;
        }

        public void setRemovedSpecialCharacters(int removedSpecialCharacters) {
            this.removedSpecialCharacters = removedSpecialCharacters;
        }

        public double getCompressionRatio() {
            return compressionRatio;
        }

        public void setCompressionRatio(double compressionRatio) {
            this.compressionRatio = compressionRatio;
        }

        public int getWordCount() {
            return wordCount;
        }

        public void setWordCount(int wordCount) {
            this.wordCount = wordCount;
        }

        public int getSentenceCount() {
            return sentenceCount;
        }

        public void setSentenceCount(int sentenceCount) {
            this.sentenceCount = sentenceCount;
        }

        public int getParagraphCount() {
            return paragraphCount;
        }

        public void setParagraphCount(int paragraphCount) {
            this.paragraphCount = paragraphCount;
        }
    }

    public static class NormalizationMetadata {
        private String normalizationVersion;
        private String processorVersion;
        private String algorithm;
        private Map<String, Object> configuration;
        private String checksum;
        private LocalDateTime createdAt;

        public NormalizationMetadata() {
        }

        public NormalizationMetadata(String normalizationVersion, String processorVersion, String algorithm,
                                     Map<String, Object> configuration, String checksum, LocalDateTime createdAt) {
            this.normalizationVersion = normalizationVersion;
            this.processorVersion = processorVersion;
            this.algorithm = algorithm;
            this.configuration = configuration;
            this.checksum = checksum;
            this.createdAt = createdAt;
        }

        public String getNormalizationVersion() {
            return normalizationVersion;
        }

        public void setNormalizationVersion(String normalizationVersion) {
            this.normalizationVersion = normalizationVersion;
        }

        public String getProcessorVersion() {
            return processorVersion;
        }

        public void setProcessorVersion(String processorVersion) {
            this.processorVersion = processorVersion;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public Map<String, Object> getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Map<String, Object> configuration) {
            this.configuration = configuration;
        }

        public String getChecksum() {
            return checksum;
        }

        public void setChecksum(String checksum) {
            this.checksum = checksum;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private News originalNews;
        private NormalizedContent normalizedContent;
        private boolean success;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private long processingTimeMs;
        private Map<String, Long> stepProcessingTimes;
        private List<NormalizationWarning> warnings;
        private List<NormalizationError> errors;
        private NormalizationStatistics statistics;
        private NormalizationMetadata metadata;

        public Builder originalNews(News originalNews) {
            this.originalNews = originalNews;
            return this;
        }

        public Builder normalizedContent(NormalizedContent normalizedContent) {
            this.normalizedContent = normalizedContent;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder stepProcessingTimes(Map<String, Long> stepProcessingTimes) {
            this.stepProcessingTimes = stepProcessingTimes;
            return this;
        }

        public Builder warnings(List<NormalizationWarning> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder errors(List<NormalizationError> errors) {
            this.errors = errors;
            return this;
        }

        public Builder statistics(NormalizationStatistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Builder metadata(NormalizationMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public ContentNormalizationResult build() {
            return new ContentNormalizationResult(originalNews, normalizedContent, success, startedAt, completedAt,
                    processingTimeMs, stepProcessingTimes, warnings, errors, statistics, metadata);
        }
    }
}