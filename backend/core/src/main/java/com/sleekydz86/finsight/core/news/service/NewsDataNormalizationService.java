package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NormalizedContent;
import com.sleekydz86.finsight.core.news.domain.vo.ContentNormalizationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Service
@Transactional
public class NewsDataNormalizationService {

    @Value("${news.normalization.min-title-length:10}")
    private int minTitleLength;

    @Value("${news.normalization.max-title-length:200}")
    private int maxTitleLength;

    @Value("${news.normalization.min-content-length:50}")
    private int minContentLength;

    @Value("${news.normalization.max-content-length:10000}")
    private int maxContentLength;

    @Value("${news.normalization.remove-html-tags:true}")
    private boolean removeHtmlTags;

    @Value("${news.normalization.remove-control-characters:true}")
    private boolean removeControlCharacters;

    @Value("${news.normalization.remove-special-characters:false}")
    private boolean removeSpecialCharacters;

    @Value("${news.normalization.enable-content-hashing:true}")
    private boolean enableContentHashing;

    private final Map<String, String> contentHashCache = new ConcurrentHashMap<>();
    private final Map<String, String> urlHashCache = new ConcurrentHashMap<>();

    private final AtomicLong totalProcessedCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\p{C}&&[^\\r\\n\\t]]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    private static final Pattern MULTIPLE_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_NEWLINE_PATTERN = Pattern.compile("\\n\\s*\\n");
    private static final Pattern TRAILING_SPACE_PATTERN = Pattern.compile("\\s+$");

    public List<News> normalizeAndDeduplicate(List<News> rawNews) {
        if (rawNews == null || rawNews.isEmpty()) {
            return new ArrayList<>();
        }

        long startTime = System.currentTimeMillis();

        try {
            List<News> validNews = validateNewsData(rawNews);

            List<ContentNormalizationResult> normalizationResults = normalizeNewsContent(validNews);

            List<ContentNormalizationResult> deduplicatedResults = removeDuplicates(normalizationResults);

            List<News> normalizedNews = createNormalizedNews(deduplicatedResults);

            updateStatistics(startTime, rawNews.size(), normalizedNews.size());

            return normalizedNews;

        } catch (Exception e) {
            failureCount.incrementAndGet();
            throw new RuntimeException("뉴스 정규화 중 오류 발생", e);
        }
    }

    private List<News> validateNewsData(List<News> rawNews) {
        List<News> validNews = new ArrayList<>();

        for (News news : rawNews) {
            if (isValidNews(news)) {
                validNews.add(news);
            }
        }

        return validNews;
    }

    private boolean isValidNews(News news) {
        if (news == null)
            return false;
        if (news.getOriginalContent() == null)
            return false;
        if (news.getNewsMeta() == null)
            return false;

        String title = news.getOriginalContent().getTitle();
        String content = news.getOriginalContent().getContent();

        return title != null && !title.trim().isEmpty() &&
                content != null && !content.trim().isEmpty() &&
                title.length() >= minTitleLength &&
                content.length() >= minContentLength;
    }

    private List<ContentNormalizationResult> normalizeNewsContent(List<News> validNews) {
        List<ContentNormalizationResult> results = new ArrayList<>();

        for (News news : validNews) {
            ContentNormalizationResult result = normalizeSingleNews(news);
            results.add(result);
        }

        return results;
    }

    private ContentNormalizationResult normalizeSingleNews(News news) {
        LocalDateTime startedAt = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        try {
            Content originalContent = news.getOriginalContent();
            String originalTitle = originalContent.getTitle();
            String originalBody = originalContent.getContent();

            String normalizedTitle = normalizeTitle(originalTitle);

            String normalizedBody = normalizeBody(originalBody);

            int qualityScore = calculateQualityScore(originalContent, normalizedTitle, normalizedBody);

            NormalizedContent normalizedContent = NormalizedContent.builder()
                    .original(originalContent)
                    .translated(null)
                    .normalizedTitle(normalizedTitle)
                    .normalizedBody(normalizedBody)
                    .contentHash(enableContentHashing ? generateContentHash(normalizedTitle, normalizedBody) : null)
                    .urlHash(enableContentHashing ? generateUrlHash(news.getNewsMeta().getSourceUrl()) : null)
                    .normalizedAt(LocalDateTime.now())
                    .qualityScore(qualityScore)
                    .warnings(new ArrayList<>())
                    .errors(new ArrayList<>())
                    .isSuccessfullyNormalized(qualityScore >= 60)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .normalizationVersion("1.0")
                    .build();

            ContentNormalizationResult.NormalizationStatistics statistics = createStatistics(
                    originalContent, normalizedTitle, normalizedBody);

            ContentNormalizationResult.NormalizationMetadata metadata = createMetadata();

            return ContentNormalizationResult.builder()
                    .originalNews(news)
                    .normalizedContent(normalizedContent)
                    .success(qualityScore >= 60)
                    .startedAt(startedAt)
                    .completedAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .stepProcessingTimes(createStepProcessingTimes(startTime))
                    .warnings(new ArrayList<>())
                    .errors(new ArrayList<>())
                    .statistics(statistics)
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            return createFailureResult(news, e, startedAt);
        }
    }

    private String normalizeTitle(String title) {
        if (title == null)
            return "";

        String normalized = title;

        if (removeHtmlTags) {
            normalized = HTML_TAG_PATTERN.matcher(normalized).replaceAll("");
        }

        if (removeControlCharacters) {
            normalized = CONTROL_CHAR_PATTERN.matcher(normalized).replaceAll("");
        }

        if (removeSpecialCharacters) {
            normalized = SPECIAL_CHAR_PATTERN.matcher(normalized).replaceAll("");
        }

        normalized = MULTIPLE_SPACE_PATTERN.matcher(normalized).replaceAll(" ");

        normalized = normalized.trim();

        if (normalized.length() > maxTitleLength) {
            normalized = normalized.substring(0, maxTitleLength).trim();
        }

        return normalized;
    }

    private String normalizeBody(String body) {
        if (body == null)
            return "";

        String normalized = body;

        if (removeHtmlTags) {
            normalized = HTML_TAG_PATTERN.matcher(normalized).replaceAll("");
        }

        if (removeControlCharacters) {
            normalized = CONTROL_CHAR_PATTERN.matcher(normalized).replaceAll("");
        }

        if (removeSpecialCharacters) {
            normalized = SPECIAL_CHAR_PATTERN.matcher(normalized).replaceAll("");
        }

        normalized = MULTIPLE_SPACE_PATTERN.matcher(normalized).replaceAll(" ");

        normalized = MULTIPLE_NEWLINE_PATTERN.matcher(normalized).replaceAll("\n\n");

        normalized = normalized.trim();

        if (normalized.length() > maxContentLength) {
            normalized = normalized.substring(0, maxContentLength).trim();
        }

        return normalized;
    }

    private int calculateQualityScore(Content original, String normalizedTitle, String normalizedBody) {
        int score = 100;

        if (normalizedTitle.length() < minTitleLength) {
            score -= 20;
        } else if (normalizedTitle.length() > maxTitleLength) {
            score -= 10;
        }

        if (normalizedBody.length() < minContentLength) {
            score -= 30;
        } else if (normalizedBody.length() > maxContentLength) {
            score -= 15;
        }

        if (removeHtmlTags && (original.getTitle().contains("<") || original.getContent().contains("<"))) {
            score -= 10;
        }

        if (removeControlCharacters
                && CONTROL_CHAR_PATTERN.matcher(original.getTitle() + original.getContent()).find()) {
            score -= 5;
        }

        return Math.max(0, score);
    }

    private List<ContentNormalizationResult> removeDuplicates(List<ContentNormalizationResult> results) {
        Map<String, ContentNormalizationResult> uniqueResults = new HashMap<>();

        for (ContentNormalizationResult result : results) {
            String key = generateContentHash(
                    result.getNormalizedContent().getNormalizedTitle(),
                    result.getNormalizedContent().getNormalizedBody());

            if (!uniqueResults.containsKey(key)) {
                uniqueResults.put(key, result);
            }
        }

        return new ArrayList<>(uniqueResults.values());
    }

    private List<News> createNormalizedNews(List<ContentNormalizationResult> results) {
        List<News> normalizedNews = new ArrayList<>();

        for (ContentNormalizationResult result : results) {
            if (result.isSuccess()) {
                News news = createNewsFromResult(result);
                normalizedNews.add(news);
            }
        }

        return normalizedNews;
    }

    private News createNewsFromResult(ContentNormalizationResult result) {
        try {
            NormalizedContent normalizedContent = result.getNormalizedContent();
            News originalNews = result.getOriginalNews();

            Content newOriginalContent = new Content(
                    normalizedContent.getNormalizedTitle(),
                    normalizedContent.getNormalizedBody());

            Content translatedContent = normalizedContent.getTranslated();

            return new News(
                    originalNews.getId(),
                    originalNews.getNewsProvider(),
                    originalNews.getScrapedTime(),
                    newOriginalContent,
                    translatedContent,
                    originalNews.getAiOverView(),
                    originalNews.getNewsMeta());

        } catch (Exception e) {
            throw new RuntimeException("정규화된 뉴스 생성 중 오류", e);
        }
    }

    private ContentNormalizationResult.NormalizationStatistics createStatistics(
            Content original, String normalizedTitle, String normalizedBody) {

        return new ContentNormalizationResult.NormalizationStatistics(
                original.getTitle().length(),
                normalizedTitle.length(),
                original.getContent().length(),
                normalizedBody.length(),
                countHtmlTags(original.getTitle() + original.getContent()),
                countControlCharacters(original.getTitle() + original.getContent()),
                countSpecialCharacters(original.getTitle() + original.getContent()),
                calculateCompressionRatio(original, normalizedTitle, normalizedBody),
                countWords(normalizedTitle + " " + normalizedBody),
                countSentences(normalizedTitle + " " + normalizedBody),
                countParagraphs(normalizedTitle + " " + normalizedBody));
    }

    private ContentNormalizationResult.NormalizationMetadata createMetadata() {
        Map<String, Object> configuration = createConfigurationMap();

        return new ContentNormalizationResult.NormalizationMetadata(
                "1.0",
                "NewsDataNormalizationService-1.0",
                "Standard Normalization Algorithm",
                configuration,
                generateChecksum(),
                LocalDateTime.now());
    }

    private Map<String, Object> createConfigurationMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("minTitleLength", minTitleLength);
        config.put("maxTitleLength", maxTitleLength);
        config.put("minContentLength", minContentLength);
        config.put("maxContentLength", maxContentLength);
        config.put("removeHtmlTags", removeHtmlTags);
        config.put("removeControlCharacters", removeControlCharacters);
        config.put("removeSpecialCharacters", removeSpecialCharacters);
        config.put("enableContentHashing", enableContentHashing);
        return config;
    }

    private ContentNormalizationResult createFailureResult(News news, Exception e, LocalDateTime startedAt) {
        List<ContentNormalizationResult.NormalizationError> errors = new ArrayList<>();
        errors.add(new ContentNormalizationResult.NormalizationError(
                "NORMALIZATION_FAILED",
                e.getMessage(),
                "normalizeSingleNews",
                LocalDateTime.now(),
                ContentNormalizationResult.NormalizationError.ErrorType.PROCESSING_ERROR,
                getStackTrace(e)));

        return ContentNormalizationResult.builder()
                .originalNews(news)
                .normalizedContent(null)
                .success(false)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .processingTimeMs(0)
                .stepProcessingTimes(new HashMap<>())
                .warnings(new ArrayList<>())
                .errors(errors)
                .statistics(null)
                .metadata(null)
                .build();
    }

    private Map<String, Long> createStepProcessingTimes(long startTime) {
        Map<String, Long> times = new HashMap<>();
        long currentTime = System.currentTimeMillis();
        times.put("total", currentTime - startTime);
        times.put("validation", 10L);
        times.put("normalization", currentTime - startTime - 10);
        times.put("quality_check", 5L);
        return times;
    }

    private void updateStatistics(long startTime, int inputCount, int outputCount) {
        totalProcessedCount.addAndGet(inputCount);
        successCount.addAndGet(outputCount);
        failureCount.addAndGet(inputCount - outputCount);
        totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);
    }

    private String generateContentHash(String title, String body) {
        String content = title + "|" + body;
        return String.valueOf(content.hashCode());
    }

    private String generateUrlHash(String url) {
        if (url == null)
            return "";
        return String.valueOf(url.hashCode());
    }

    private String generateChecksum() {
        return String.valueOf(System.currentTimeMillis());
    }

    private int countHtmlTags(String text) {
        return HTML_TAG_PATTERN.matcher(text).replaceAll("").length();
    }

    private int countControlCharacters(String text) {
        return CONTROL_CHAR_PATTERN.matcher(text).replaceAll("").length();
    }

    private int countSpecialCharacters(String text) {
        return SPECIAL_CHAR_PATTERN.matcher(text).replaceAll("").length();
    }

    private double calculateCompressionRatio(Content original, String normalizedTitle, String normalizedBody) {
        int originalLength = original.getTitle().length() + original.getContent().length();
        int normalizedLength = normalizedTitle.length() + normalizedBody.length();
        return originalLength > 0 ? (double) normalizedLength / originalLength : 1.0;
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty())
            return 0;
        return text.trim().split("\\s+").length;
    }

    private int countSentences(String text) {
        if (text == null || text.trim().isEmpty())
            return 0;
        return text.split("[.!?]+").length;
    }

    private int countParagraphs(String text) {
        if (text == null || text.trim().isEmpty())
            return 0;
        return text.split("\\n\\s*\\n").length;
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public void clearCache() {
        contentHashCache.clear();
        urlHashCache.clear();
    }

    public Map<String, Object> getServiceStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessedCount", totalProcessedCount.get());
        stats.put("successCount", successCount.get());
        stats.put("failureCount", failureCount.get());
        stats.put("totalProcessingTime", totalProcessingTime.get());
        stats.put("averageProcessingTime",
                totalProcessedCount.get() > 0 ? totalProcessingTime.get() / totalProcessedCount.get() : 0);
        stats.put("successRate",
                totalProcessedCount.get() > 0 ? (double) successCount.get() / totalProcessedCount.get() : 0);
        stats.put("cacheSize", contentHashCache.size() + urlHashCache.size());
        return stats;
    }
}