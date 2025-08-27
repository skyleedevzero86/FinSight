package com.sleekydz86.finsight.core.news.domain.port.out;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.Content;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NewsAiAnalysisRequesterPort {
    List<News> analyseNewses(AiModel model, Content content);
    CompletableFuture<List<News>> analyseNewsesAsync(AiModel model, Content content);
    List<News> analyseNewsesBatch(AiModel model, List<Content> contents);
    boolean isModelAvailable(AiModel model);
    List<AiModel> getAvailableModels();
}