package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.news.domain.vo.SentimentAnalysisResult;

public interface SentimentAnalysisPort {

    SentimentAnalysisResult analyzeSentiment(String text);

    boolean isModelAvailable();
}