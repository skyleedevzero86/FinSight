package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.news.domain.port.out.requester.NewsAiRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Comparator;

@Service
public class AiModelSelectionService {

    private static final Logger log = LoggerFactory.getLogger(AiModelSelectionService.class);

    private final Map<AiModel, NewsAiRequester> aiRequesters;
    private final Map<AiModel, AtomicLong> modelUsageCount = new ConcurrentHashMap<>();
    private final Map<AiModel, AtomicLong> modelErrorCount = new ConcurrentHashMap<>();

    @Value("${ai.default-model:CHATGPT}")
    private String defaultModelName;

    @Value("${ai.fallback-model:CHATGPT}")
    private String fallbackModelName;

    public AiModelSelectionService(List<NewsAiRequester> requesters) {
        this.aiRequesters = requesters.stream()
                .collect(ConcurrentHashMap::new,
                        (map, requester) -> map.put(requester.supports(), requester),
                        ConcurrentHashMap::putAll);

        for (AiModel model : AiModel.values()) {
            modelUsageCount.put(model, new AtomicLong(0));
            modelErrorCount.put(model, new AtomicLong(0));
        }
    }

    public NewsAiRequester selectModel(AiModel model) {
        NewsAiRequester requester = aiRequesters.get(model);
        if (requester == null) {
            log.warn("Requested AI model {} is not available, falling back to {}", model, fallbackModelName);
            return selectFallbackModel();
        }
        return requester;
    }

    public NewsAiRequester selectModelByContentLength(String content, int threshold) {
        if (content.length() > threshold) {
            return selectModel(AiModel.CHATGPT);
        } else {
            return selectModel(AiModel.GEMMA);
        }
    }

    public NewsAiRequester selectModelByPriority() {
        AiModel[] priorityOrder = {
                AiModel.CHATGPT, AiModel.CLAUDE, AiModel.GEMINI,
                AiModel.GROK, AiModel.LLAMA, AiModel.GEMMA
        };

        for (AiModel model : priorityOrder) {
            NewsAiRequester requester = aiRequesters.get(model);
            if (requester != null) {
                return requester;
            }
        }

        return selectFallbackModel();
    }

    public NewsAiRequester selectModelByLoadBalancing() {
        return modelUsageCount.entrySet().stream()
                .filter(entry -> aiRequesters.containsKey(entry.getKey()))
                .min(Comparator.comparingLong(entry -> entry.getValue().get()))
                .map(Map.Entry::getKey)
                .map(this::selectModel)
                .orElse(selectFallbackModel());
    }

    public NewsAiRequester selectModelByErrorRate() {
        return modelErrorCount.entrySet().stream()
                .filter(entry -> aiRequesters.containsKey(entry.getKey()))
                .min(Comparator.comparingDouble(entry -> getErrorRate(entry.getKey())))
                .map(Map.Entry::getKey)
                .map(this::selectModel)
                .orElse(selectFallbackModel());
    }

    private NewsAiRequester selectFallbackModel() {
        AiModel fallbackModel = AiModel.valueOf(fallbackModelName);
        NewsAiRequester fallbackRequester = aiRequesters.get(fallbackModel);

        if (fallbackRequester == null) {
            return aiRequesters.values().iterator().next();
        }

        return fallbackRequester;
    }

    public void recordModelUsage(AiModel model) {
        modelUsageCount.computeIfPresent(model, (k, v) -> {
            v.incrementAndGet();
            return v;
        });
    }

    public void recordModelError(AiModel model) {
        modelErrorCount.computeIfPresent(model, (k, v) -> {
            v.incrementAndGet();
            return v;
        });
    }

    private double getErrorRate(AiModel model) {
        AtomicLong usage = modelUsageCount.get(model);
        AtomicLong errors = modelErrorCount.get(model);

        if (usage.get() == 0)
            return 0.0;
        return (double) errors.get() / usage.get();
    }

    public List<AiModel> getAvailableModels() {
        return aiRequesters.keySet().stream().toList();
    }

    public Map<AiModel, Long> getModelUsageStats() {
        return modelUsageCount.entrySet().stream()
                .collect(ConcurrentHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                        ConcurrentHashMap::putAll);
    }

    public Map<AiModel, Double> getModelErrorRates() {
        return modelUsageCount.entrySet().stream()
                .collect(ConcurrentHashMap::new,
                        (map, entry) -> {
                            AiModel model = entry.getKey();
                            double errorRate = getErrorRate(model);
                            map.put(model, errorRate);
                        },
                        ConcurrentHashMap::putAll);
    }

    public void resetModelStats() {
        modelUsageCount.values().forEach(atomicLong -> atomicLong.set(0));
        modelErrorCount.values().forEach(atomicLong -> atomicLong.set(0));
        log.info("AI model statistics reset");
    }
}