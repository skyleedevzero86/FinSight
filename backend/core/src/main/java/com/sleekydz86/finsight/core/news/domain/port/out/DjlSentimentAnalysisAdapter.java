package com.sleekydz86.finsight.core.news.domain.port.out;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import com.sleekydz86.finsight.core.news.adapter.out.DjlThreadPredictor;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.DjlClassProbability;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentResult;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class DjlSentimentAnalysisAdapter implements DjlSentimentAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(DjlSentimentAnalysisAdapter.class);
    private static final String DISTILBERT = "distilbert-base-uncased-finetuned-sst-2-english";

    @Value("${ai.djl.model.name:cardiffnlp/twitter-roberta-base-sentiment-latest}")
    private String modelName;

    @Value("${ai.djl.enabled:true}")
    private boolean djlEnabled;

    private ZooModel<String, Classifications> model;
    private DjlThreadPredictor<String, Classifications> predictors;
    private boolean modelAvailable = false;
    private String activeModelName;

    @PostConstruct
    public void initialize() {
        if (!djlEnabled) {
            log.info("DJL 모델이 비활성화되었습니다.");
            return;
        }
        try {
            loadModel();
            predictors = new DjlThreadPredictor<>(model);
            modelAvailable = true;
            log.info("DJL 감정분석 모델 로드 완료: {}", activeModelName);
        } catch (Exception e) {
            log.error("DJL 감정분석 모델 로드 실패, 폴백 모드로 전환: {}", e.getMessage());
            modelAvailable = false;
        }
    }

    private void loadModel() throws ModelNotFoundException, MalformedModelException, IOException {
        try {
            model = criteria("djl://ai.djl.huggingface.pytorch/" + modelName).loadModel();
            activeModelName = modelName;
            log.info("HuggingFace 모델 로드 성공: {}", modelName);
            return;
        } catch (Exception e) {
            log.warn("HuggingFace 모델 로드 실패, 로컬 모델 시도: {}", e.getMessage());
        }
        try {
            model = Criteria.builder()
                    .optApplication(Application.NLP.SENTIMENT_ANALYSIS)
                    .setTypes(String.class, Classifications.class)
                    .optFilter("backbone", "distilbert")
                    .optEngine("PyTorch")
                    .build()
                    .loadModel();
            activeModelName = DISTILBERT;
            log.info("로컬 DistilBERT 모델 로드 성공");
        } catch (Exception e) {
            log.error("모든 모델 로드 방법 실패: {}", e.getMessage());
            throw e;
        }
    }

    private Criteria<String, Classifications> criteria(String modelUrl) {
        return Criteria.builder()
                .setTypes(String.class, Classifications.class)
                .optModelUrls(modelUrl)
                .optEngine("PyTorch")
                .optOption("translatorFactory", "ai.djl.huggingface.translator.TextClassificationTranslatorFactory")
                .build();
    }

    @Override
    public DjlSentimentResult analyzeSentiment(String text) {
        if (!ready()) {
            return createFallbackResult(text, "모델을 사용할 수 없습니다");
        }
        long startTime = System.currentTimeMillis();
        try {
            return toResult(text, predictors.borrow().predict(text), startTime);
        } catch (Exception e) {
            log.error("감정분석 처리 실패: {}", e.getMessage());
            return createFallbackResult(text, e.getMessage());
        }
    }

    @Override
    public CompletableFuture<DjlSentimentResult> analyzeSentimentAsync(String text) {
        return CompletableFuture.supplyAsync(() -> analyzeSentiment(text));
    }

    @Override
    public List<DjlSentimentResult> analyzeSentimentBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (!ready()) {
            return texts.stream().map(text -> createFallbackResult(text, "모델을 사용할 수 없습니다")).toList();
        }
        long startTime = System.currentTimeMillis();
        try {
            List<Classifications> outputs = predictors.borrow().batchPredict(texts);
            return mapBatch(texts, outputs, startTime);
        } catch (Exception e) {
            log.warn("DJL 배치 추론에 실패해 건별로 다시 시도합니다. 건수={}", texts.size(), e);
            return texts.stream().map(this::analyzeSentiment).toList();
        }
    }

    @Override
    public DjlSentimentResult analyzeNewsContent(Content content) {
        if (content == null) {
            return createFallbackResult("", "콘텐츠가 null입니다");
        }
        String title = content.getTitle() == null ? "" : content.getTitle();
        String body = content.getContent() == null ? "" : content.getContent();
        return analyzeSentiment(title + ". " + body);
    }

    @Override
    public boolean isModelAvailable() {
        return ready();
    }

    @Override
    public List<String> getAvailableModels() {
        return Arrays.asList(modelName, DISTILBERT);
    }

    @Override
    public Map<String, Object> getModelMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelName", activeModelName == null ? modelName : activeModelName);
        metadata.put("isAvailable", isModelAvailable());
        metadata.put("enabled", djlEnabled);
        return metadata;
    }

    private boolean ready() {
        return modelAvailable && model != null && predictors != null;
    }

    private List<DjlSentimentResult> mapBatch(List<String> texts, List<Classifications> outputs, long startTime) {
        List<DjlSentimentResult> results = new ArrayList<>();
        for (int index = 0; index < texts.size(); index++) {
            if (outputs == null || index >= outputs.size() || outputs.get(index) == null) {
                results.add(createFallbackResult(texts.get(index), "모델이 라벨을 반환하지 않았습니다"));
                continue;
            }
            results.add(toResult(texts.get(index), outputs.get(index), startTime));
        }
        return results;
    }

    private DjlSentimentResult toResult(String text, Classifications classifications, long startTime) {
        List<DjlClassProbability> classes = new ArrayList<>();
        for (Classifications.Classification item : classifications.items()) {
            classes.add(new DjlClassProbability(item.getClassName(), item.getProbability()));
        }
        if (classes.isEmpty()) {
            return createFallbackResult(text, "모델이 라벨을 반환하지 않았습니다");
        }
        DjlSentimentScore score = DjlSentimentScore.from(classes);
        return DjlSentimentResult.builder()
                .label(score.type().name())
                .score(score.direction())
                .confidence(score.confidence())
                .positiveProbability(score.positive())
                .neutralProbability(score.neutral())
                .negativeProbability(score.negative())
                .success(true)
                .modelName(activeModelName)
                .originalText(text)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private DjlSentimentResult createFallbackResult(String text, String errorMessage) {
        return DjlSentimentResult.builder()
                .label("NEUTRAL")
                .score(0.0)
                .confidence(0.0)
                .success(false)
                .errorMessage(errorMessage)
                .modelName(activeModelName == null ? modelName : activeModelName)
                .originalText(text)
                .processingTimeMs(0L)
                .build();
    }

    @PreDestroy
    public void cleanup() {
        if (predictors != null) {
            predictors.close();
        }
        if (model != null) {
            try {
                model.close();
                log.info("DJL 모델 리소스 정리 완료");
            } catch (Exception e) {
                log.error("모델 리소스 정리 중 오류: {}", e.getMessage());
            }
        }
    }
}
