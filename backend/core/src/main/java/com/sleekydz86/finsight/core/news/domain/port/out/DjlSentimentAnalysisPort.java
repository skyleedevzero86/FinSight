package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

public interface DjlSentimentAnalysisPort {

    DjlSentimentResult analyzeSentiment(String text);

    CompletableFuture<DjlSentimentResult> analyzeSentimentAsync(String text);

    List<DjlSentimentResult> analyzeSentimentBatch(List<String> texts);

    DjlSentimentResult analyzeNewsContent(Content content);

    boolean isModelAvailable();

    List<String> getAvailableModels();

    Map<String, Object> getModelMetadata();
}