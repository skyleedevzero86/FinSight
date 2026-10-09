package com.sleekydz86.finsight.core.news.domain.vo;

import java.util.List;

public record DjlSentimentScore(
        SentimentType type,
        double direction,
        double confidence,
        double positive,
        double neutral,
        double negative
) {

    public static DjlSentimentScore from(List<DjlClassProbability> classes) {
        double positive = 0;
        double neutral = 0;
        double negative = 0;
        double confidence = 0;
        String bestName = null;
        for (DjlClassProbability item : classes) {
            SentimentType type = DjlSentimentLabels.toType(item.className());
            if (type == SentimentType.POSITIVE) {
                positive += item.probability();
            } else if (type == SentimentType.NEGATIVE) {
                negative += item.probability();
            } else {
                neutral += item.probability();
            }
            if (bestName == null || item.probability() > confidence) {
                confidence = item.probability();
                bestName = item.className();
            }
        }
        return new DjlSentimentScore(
                DjlSentimentLabels.toType(bestName),
                positive - negative,
                confidence,
                positive,
                neutral,
                negative);
    }
}
