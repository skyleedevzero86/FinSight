package com.sleekydz86.finsight.core.news.adapter.out;

import com.sleekydz86.finsight.core.news.domain.port.out.SentimentAnalysisPort;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentAnalysisResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SentimentAnalysisAdapter implements SentimentAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(SentimentAnalysisAdapter.class);

    private final AtomicBoolean modelAvailable = new AtomicBoolean(true);
    private final AtomicLong totalAnalysisCount = new AtomicLong(0);
    private final AtomicLong successfulAnalysisCount = new AtomicLong(0);

    private static final List<String> POSITIVE_KEYWORDS = Arrays.asList(
            "상승", "성장", "증가", "개선", "향상", "긍정", "좋음", "유리", "호재", "돌파",
            "기대", "희망", "성공", "승리", "상향", "강세", "회복", "반등", "급등", "상한가",
            "최고", "신고가", "호조", "활황", "상승세", "호황", "번영", "발전", "혁신", "투자"
    );

    private static final List<String> NEGATIVE_KEYWORDS = Arrays.asList(
            "하락", "감소", "악화", "부정", "나쁨", "불리", "악재", "하향", "약세", "폭락",
            "위험", "위기", "실패", "손실", "부도", "파산", "폐쇄", "해고", "감원", "부실",
            "최저", "신저가", "부진", "침체", "하락세", "불황", "파괴", "후퇴", "손해", "적자"
    );

    private static final List<String> NEUTRAL_KEYWORDS = Arrays.asList(
            "유지", "보합", "변동없음", "현상유지", "안정", "평형", "균형", "일반", "보통",
            "중간", "평범", "예상대로", "계획대로", "정상", "규칙적", "일상", "통상"
    );

    public SentimentAnalysisAdapter() {
        log.info("SentimentAnalysisAdapter 초기화 완료 - 키워드 기반 감정 분석 준비");
    }

    @Override
    public SentimentAnalysisResult analyzeSentiment(String text) {
        totalAnalysisCount.incrementAndGet();

        try {
            if (text == null || text.trim().isEmpty()) {
                log.warn("감정 분석 요청된 텍스트가 비어있습니다.");
                return SentimentAnalysisResult.failure("텍스트가 비어있습니다.");
            }

            long startTime = System.currentTimeMillis();
            log.debug("감정 분석 시작: 텍스트 길이 = {}", text.length());

            SentimentAnalysisDetails details = performSentimentAnalysis(text);
            String description = generateDescription(details);

            long processingTime = System.currentTimeMillis() - startTime;
            successfulAnalysisCount.incrementAndGet();

            log.debug("감정 분석 완료: 타입={}, 점수={}, 처리시간={}ms",
                    details.sentimentType, details.score, processingTime);

            return SentimentAnalysisResult.success(
                    details.sentimentType,
                    details.score,
                    processingTime,
                    description
            );

        } catch (Exception e) {
            log.error("감정 분석 중 예외 발생: {}", e.getMessage(), e);
            return SentimentAnalysisResult.failure("감정 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public boolean isModelAvailable() {
        return modelAvailable.get();
    }

    public String getModelInfo() {
        return "Keyword-based Sentiment Analysis Model v1.0";
    }

    public String getModelVersion() {
        return "1.0.0-RELEASE";
    }

    private SentimentAnalysisDetails performSentimentAnalysis(String text) {
        String lowerText = text.toLowerCase();

        int positiveScore = calculateKeywordScore(lowerText, POSITIVE_KEYWORDS);
        int negativeScore = calculateKeywordScore(lowerText, NEGATIVE_KEYWORDS);
        int neutralScore = calculateKeywordScore(lowerText, NEUTRAL_KEYWORDS);

        SentimentType sentimentType = determineSentimentType(positiveScore, negativeScore, neutralScore);

        double confidence = calculateConfidenceScore(positiveScore, negativeScore, neutralScore, sentimentType);

        return new SentimentAnalysisDetails(sentimentType, confidence, positiveScore, negativeScore, neutralScore);
    }

    private int calculateKeywordScore(String text, List<String> keywords) {
        return (int) keywords.stream()
                .mapToLong(keyword -> countOccurrences(text, keyword))
                .sum();
    }

    private long countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    private SentimentType determineSentimentType(int positiveScore, int negativeScore, int neutralScore) {
        if (positiveScore > negativeScore && positiveScore > neutralScore) {
            return SentimentType.POSITIVE;
        } else if (negativeScore > positiveScore && negativeScore > neutralScore) {
            return SentimentType.NEGATIVE;
        } else {
            return SentimentType.NEUTRAL;
        }
    }

    private double calculateConfidenceScore(int positiveScore, int negativeScore, int neutralScore, SentimentType type) {
        int totalScore = positiveScore + negativeScore + neutralScore;

        if (totalScore == 0) {
            return 0.5;
        }

        double baseScore;
        int dominantScore;

        switch (type) {
            case POSITIVE:
                dominantScore = positiveScore;
                baseScore = 0.5 + (0.4 * ((double) dominantScore / totalScore));
                break;
            case NEGATIVE:
                dominantScore = negativeScore;
                baseScore = 0.5 - (0.4 * ((double) dominantScore / totalScore));
                break;
            case NEUTRAL:
            default:
                baseScore = 0.5;
                break;
        }

        return Math.max(0.1, Math.min(0.9, baseScore));
    }

    private String generateDescription(SentimentAnalysisDetails details) {
        StringBuilder description = new StringBuilder();

        switch (details.sentimentType) {
            case POSITIVE:
                if (details.score >= 0.8) {
                    description.append("매우 긍정적인 뉴스입니다. ");
                } else if (details.score >= 0.6) {
                    description.append("긍정적인 뉴스입니다. ");
                } else {
                    description.append("약간 긍정적인 뉴스입니다. ");
                }
                description.append("시장에 호재로 작용할 것으로 예상됩니다.");
                break;

            case NEGATIVE:
                if (details.score <= 0.2) {
                    description.append("매우 부정적인 뉴스입니다. ");
                } else if (details.score <= 0.4) {
                    description.append("부정적인 뉴스입니다. ");
                } else {
                    description.append("약간 부정적인 뉴스입니다. ");
                }
                description.append("시장에 악재로 작용할 수 있습니다.");
                break;

            case NEUTRAL:
            default:
                description.append("중립적인 뉴스입니다. ");
                description.append("시장에 큰 영향을 주지 않을 것으로 보입니다.");
                break;
        }

        description.append(String.format(" (긍정 키워드: %d개, 부정 키워드: %d개, 중립 키워드: %d개)",
                details.positiveKeywordCount, details.negativeKeywordCount, details.neutralKeywordCount));

        return description.toString();
    }

    public void setModelAvailable(boolean available) {
        modelAvailable.set(available);
        log.info("감정 분석 모델 가용성 상태 변경: {}", available);
    }

    public AnalysisStatistics getStatistics() {
        long total = totalAnalysisCount.get();
        long successful = successfulAnalysisCount.get();
        double successRate = total > 0 ? (double) successful / total * 100 : 0.0;

        return new AnalysisStatistics(total, successful, successRate);
    }

    private static class SentimentAnalysisDetails {
        final SentimentType sentimentType;
        final double score;
        final int positiveKeywordCount;
        final int negativeKeywordCount;
        final int neutralKeywordCount;

        SentimentAnalysisDetails(SentimentType sentimentType, double score,
                                 int positiveKeywordCount, int negativeKeywordCount, int neutralKeywordCount) {
            this.sentimentType = sentimentType;
            this.score = score;
            this.positiveKeywordCount = positiveKeywordCount;
            this.negativeKeywordCount = negativeKeywordCount;
            this.neutralKeywordCount = neutralKeywordCount;
        }
    }

    public static class AnalysisStatistics {
        private final long totalAnalysisCount;
        private final long successfulAnalysisCount;
        private final double successRate;

        public AnalysisStatistics(long totalAnalysisCount, long successfulAnalysisCount, double successRate) {
            this.totalAnalysisCount = totalAnalysisCount;
            this.successfulAnalysisCount = successfulAnalysisCount;
            this.successRate = successRate;
        }

        public long getTotalAnalysisCount() { return totalAnalysisCount; }
        public long getSuccessfulAnalysisCount() { return successfulAnalysisCount; }
        public double getSuccessRate() { return successRate; }

        @Override
        public String toString() {
            return String.format("AnalysisStatistics{total=%d, successful=%d, successRate=%.2f%%}",
                    totalAnalysisCount, successfulAnalysisCount, successRate);
        }
    }
}