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
                return analyzedNewsList.get(0);
            }
        } catch (Exception e) {
            log.warn("AI 분석 실패, 원본 뉴스 사용: {}", news.getId(), e);
        }
        return news;
    }
}