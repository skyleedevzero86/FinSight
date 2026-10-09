package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsAiProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NewsAiProcessingService.class);

    private final NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort;

    public NewsAiProcessingService(NewsAiAnalysisRequesterPort newsAiAnalysisRequesterPort) {
        this.newsAiAnalysisRequesterPort = newsAiAnalysisRequesterPort;
    }

    public List<News> processNewsWithAI(List<News> newses) {
        return newses.stream()
                .map(this::analyzeNewsWithAI)
                .collect(Collectors.toList());
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

    public News processNewsWithAI(News news) {
        if (news == null) {
            return null;
        }
        return analyzeNewsWithAI(news);
    }

}