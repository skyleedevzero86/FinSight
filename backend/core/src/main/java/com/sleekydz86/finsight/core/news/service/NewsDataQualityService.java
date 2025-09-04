package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.AiOverview;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NewsDataQualityService {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int MIN_TITLE_LENGTH = 10;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MIN_CONTENT_LENGTH = 50;
    private static final int MAX_CONTENT_LENGTH = 10000;

    public DataQualityResult validateNewsQuality(News news) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        validateTitle(news.getOriginalContent().getTitle(), errors, warnings);

        validateContent(news.getOriginalContent().getContent(), errors, warnings);

        validateUrl(news.getNewsMeta().getSourceUrl(), errors, warnings);

        validateTime(news.getScrapedTime(), errors, warnings);

        if (news.getAiOverView() != null) {
            validateAiAnalysis(news.getAiOverView(), errors, warnings);
        }

        boolean isValid = errors.isEmpty();
        int qualityScore = calculateQualityScore(errors, warnings);

        return new DataQualityResult(isValid, qualityScore, errors, warnings);
    }

    private void validateTitle(String title, List<String> errors, List<String> warnings) {
        if (title == null || title.trim().isEmpty()) {
            errors.add("제목이 비어있습니다");
            return;
        }

        if (title.length() < MIN_TITLE_LENGTH) {
            errors.add("제목이 너무 짧습니다 (최소 " + MIN_TITLE_LENGTH + "자)");
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            errors.add("제목이 너무 깁니다 (최대 " + MAX_TITLE_LENGTH + "자)");
        }

        if (title.matches(".*[\\p{C}&&[^\\r\\n\\t]].*")) {
            warnings.add("제목에 제어 문자가 포함되어 있습니다");
        }

        if (title.matches(".*\\b(spam|advertisement|광고|스팸)\\b.*")) {
            warnings.add("제목에 스팸 의심 키워드가 포함되어 있습니다");
        }
    }

    private void validateContent(String content, List<String> errors, List<String> warnings) {
        if (content == null || content.trim().isEmpty()) {
            errors.add("컨텐츠가 비어있습니다");
            return;
        }

        if (content.length() < MIN_CONTENT_LENGTH) {
            errors.add("컨텐츠가 너무 짧습니다 (최소 " + MIN_CONTENT_LENGTH + "자)");
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            errors.add("컨텐츠가 너무 깁니다 (최대 " + MAX_CONTENT_LENGTH + "자)");
        }

        if (content.contains("<") || content.contains(">")) {
            warnings.add("컨텐츠에 HTML 태그가 포함되어 있습니다");
        }

        if (EMAIL_PATTERN.matcher(content).find()) {
            warnings.add("컨텐츠에 이메일 주소가 포함되어 있습니다");
        }

        if (hasDuplicateSentences(content)) {
            warnings.add("컨텐츠에 중복 문장이 포함되어 있습니다");
        }
    }

    private void validateUrl(String url, List<String> errors, List<String> warnings) {
        if (url == null || url.trim().isEmpty()) {
            errors.add("URL이 비어있습니다");
            return;
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            errors.add("유효하지 않은 URL 형식입니다");
        }

        if (url.contains("localhost") || url.contains("127.0.0.1")) {
            warnings.add("로컬호스트 URL이 포함되어 있습니다");
        }
    }

    private void validateTime(LocalDateTime scrapedTime, List<String> errors, List<String> warnings) {
        if (scrapedTime == null) {
            errors.add("스크래핑 시간이 비어있습니다");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (scrapedTime.isAfter(now)) {
            errors.add("스크래핑 시간이 미래입니다");
        }

        if (scrapedTime.isBefore(now.minusDays(365))) {
            warnings.add("스크래핑 시간이 1년 이상 과거입니다");
        }
    }

    private void validateAiAnalysis(AiOverview aiOverview, List<String> errors, List<String> warnings) {
        if (aiOverview.getOverview() == null || aiOverview.getOverview().trim().isEmpty()) {
            warnings.add("AI 분석 요약이 비어있습니다");
        }

        if (aiOverview.getSentimentScore() < -1.0 || aiOverview.getSentimentScore() > 1.0) {
            errors.add("감정 점수가 유효하지 않습니다");
        }

        if (aiOverview.getTargetCategories() == null || aiOverview.getTargetCategories().isEmpty()) {
            warnings.add("AI 분석 카테고리가 비어있습니다");
        }
    }

    private boolean hasDuplicateSentences(String content) {
        String[] sentences = content.split("[.!?]+");
        Set<String> seenSentences = new HashSet<>();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 20 && !seenSentences.add(trimmed)) {
                return true;
            }
        }

        return false;
    }

    private int calculateQualityScore(List<String> errors, List<String> warnings) {
        int baseScore = 100;
        int errorPenalty = errors.size() * 20;
        int warningPenalty = warnings.size() * 5;

        return Math.max(0, baseScore - errorPenalty - warningPenalty);
    }

    public static class DataQualityResult {
        private final LocalDateTime timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;
        private final boolean isValid;
        private final int qualityScore;
        private final List<String> errors;
        private final List<String> warnings;

        public DataQualityResult(boolean isValid, int qualityScore, List<String> errors, List<String> warnings) {
            this.timestamp = LocalDateTime.now();
            this.status = isValid ? 200 : 400;
            this.error = isValid ? null : "VALIDATION_ERROR";
            this.message = isValid ? "Validation passed" : "Validation failed";
            this.path = "news.validation";
            this.isValid = isValid;
            this.qualityScore = qualityScore;
            this.errors = errors;
            this.warnings = warnings;
        }

        public DataQualityResult(LocalDateTime timestamp, int status, String error,
                                 String message, String path, boolean isValid,
                                 int qualityScore, List<String> errors, List<String> warnings) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.isValid = isValid;
            this.qualityScore = qualityScore;
            this.errors = errors;
            this.warnings = warnings;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }

        public boolean isValid() {
            return isValid;
        }

        public int getQualityScore() {
            return qualityScore;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}