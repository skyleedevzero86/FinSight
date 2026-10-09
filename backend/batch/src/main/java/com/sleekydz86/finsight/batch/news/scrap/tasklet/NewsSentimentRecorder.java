package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.news.adapter.persistence.command.NewsJpaEntity;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentAnalysisResult;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentStatus;

import java.time.LocalDateTime;

public final class NewsSentimentRecorder {

    public static final int MAX_ATTEMPTS = 3;

    private NewsSentimentRecorder() {
    }

    public static void applyDjl(NewsJpaEntity news, DjlSentimentResult result) {
        news.setSentimentType(result.toSentimentType());
        news.setSentimentScore(result.getScore());
        news.setSentimentConfidence(result.getConfidence());
        news.setSentimentPositive(result.getPositiveProbability());
        news.setSentimentNeutral(result.getNeutralProbability());
        news.setSentimentNegative(result.getNegativeProbability());
        news.setSentimentModel(result.getModelName());
        news.setSentimentStatus(SentimentStatus.DONE);
        news.setUpdatedAt(LocalDateTime.now());
    }

    public static void applyKeyword(NewsJpaEntity news, SentimentAnalysisResult result) {
        news.setSentimentType(result.getSentimentType());
        news.setSentimentScore(result.getScore());
        news.setSentimentConfidence(result.getScore());
        news.setSentimentPositive(null);
        news.setSentimentNeutral(null);
        news.setSentimentNegative(null);
        news.setSentimentModel("keyword");
        news.setSentimentStatus(SentimentStatus.KEYWORD);
        news.setSentimentAttempts(news.getSentimentAttempts() + 1);
        news.setUpdatedAt(LocalDateTime.now());
    }

    public static void markFailed(NewsJpaEntity news) {
        news.setSentimentStatus(SentimentStatus.FAILED);
        news.setSentimentAttempts(news.getSentimentAttempts() + 1);
        news.setUpdatedAt(LocalDateTime.now());
    }
}
