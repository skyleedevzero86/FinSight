package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class NewsAiProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NewsAiProcessingService.class);

    private final NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort;
    private final Executor aiAnalysisExecutor;

    public NewsAiProcessingService(NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort,
                                   @Qualifier("aiAnalysisExecutor") Executor aiAnalysisExecutor) {
        this.newsAiAnalysisRequesterPort = newsAiAnalysisRequesterPort;
        this.aiAnalysisExecutor = aiAnalysisExecutor;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<News> processNewsWithAI(List<News> newses) {
        if (newses == null || newses.isEmpty()) {
            return List.of();
        }
        List<CompletableFuture<News>> pending = new ArrayList<>();
        for (News news : newses) {
            pending.add(CompletableFuture.supplyAsync(() -> analyzeNewsWithAI(news), aiAnalysisExecutor));
        }
        List<News> analyzed = new ArrayList<>();
        for (CompletableFuture<News> future : pending) {
            analyzed.add(future.join());
        }
        return analyzed;
    }

    private News analyzeNewsWithAI(News news) {
        try {
            Content originalContent = news.getOriginalContent();
            List<News> analyzedNewsList = newsAiAnalysisRequesterPort.analyseNewses(
                    AiModel.CHATGPT, originalContent);

            if (!analyzedNewsList.isEmpty()) {
                return keepSource(news, analyzedNewsList.get(0));
            }
        } catch (Exception e) {
            log.warn("AI 분석 실패, 원본 뉴스 사용: {}", news.getId(), e);
        }
        return news;
    }

    private News keepSource(News source, News analyzed) {
        if (analyzed.getTranslatedContent() == null || analyzed.getAiOverView() == null) {
            log.warn("AI 분석 결과가 비어 원본 뉴스를 유지합니다");
            return source;
        }
        String translatedTitle = analyzed.getTranslatedContent().getTitle();
        if (translatedTitle == null || translatedTitle.isBlank()) {
            log.warn("AI 번역 제목이 없어 원본 뉴스를 유지합니다");
            return source;
        }
        return source.updateAiAnalysis(
                analyzed.getAiOverView().getOverview(),
                translatedTitle,
                analyzed.getTranslatedContent().getContent(),
                analyzed.getAiOverView().getTargetCategories(),
                analyzed.getAiOverView().getSentimentType(),
                analyzed.getAiOverView().getSentimentScore());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public News processNewsWithAI(News news) {
        if (news == null) {
            return null;
        }
        return analyzeNewsWithAI(news);
    }

}