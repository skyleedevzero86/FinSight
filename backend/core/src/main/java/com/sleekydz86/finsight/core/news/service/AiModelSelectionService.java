package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiModelSelectionService {

    private final Map<AiModel, NewsAiRequester> aiRequesters;

    public AiModelSelectionService(List<NewsAiRequester> requesters) {
        this.aiRequesters = requesters.stream()
                .collect(Collectors.toMap(
                        NewsAiRequester::supports,
                        requester -> requester
                ));
    }

    public NewsAiRequester selectModel(AiModel model) {
        NewsAiRequester requester = aiRequesters.get(model);
        if (requester == null) {
            throw new IllegalArgumentException("지원하지 않는 AI 모델입니다: " + model);
        }
        return requester;
    }

    public NewsAiRequester selectModelByContentLength(String content, int threshold) {
        if (content.length() < threshold) {
            // 짧은 내용은 빠른 모델 (Gemma)
            return selectModel(AiModel.GEMMA);
        } else {
            // 긴 내용은 정확한 모델 (LLaMA)
            return selectModel(AiModel.LLAMA);
        }
    }

    public List<AiModel> getAvailableModels() {
        return aiRequesters.keySet().stream().toList();
    }
}