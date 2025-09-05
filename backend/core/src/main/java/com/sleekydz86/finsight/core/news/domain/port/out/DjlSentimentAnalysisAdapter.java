package com.sleekydz86.finsight.core.news.domain.port.out;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.DjlSentimentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
public class DjlSentimentAnalysisAdapter implements DjlSentimentAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(DjlSentimentAnalysisAdapter.class);

    @Value("${ai.djl.model.name:cardiffnlp/twitter-roberta-base-sentiment-latest}")
    private String modelName;

    @Value("${ai.djl.enabled:true}")
    private boolean djlEnabled;

    private ZooModel<String, ai.djl.modality.Classifications> model;
    private boolean modelAvailable = false;

    @PostConstruct
    public void initialize() {
        if (!djlEnabled) {
            log.info("DJL 모델이 비활성화되었습니다.");
            return;
        }

        try {
            loadModel();
            modelAvailable = true;
            log.info("DJL 감정분석 모델 로드 완료: {}", modelName);
        } catch (Exception e) {
            log.error("DJL 감정분석 모델 로드 실패, 폴백 모드로 전환: {}", e.getMessage());
            modelAvailable = false;
        }
    }

    private void loadModel() throws ModelNotFoundException, MalformedModelException, IOException {

        try {
            Criteria<String, ai.djl.modality.Classifications> criteria = Criteria.builder()
                    .setTypes(String.class, ai.djl.modality.Classifications.class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/" + modelName)
                    .optEngine("PyTorch")
                    .optOption("translatorFactory", "ai.djl.huggingface.translator.TextClassificationTranslatorFactory")
                    .build();

            model = criteria.loadModel();
            log.info("HuggingFace 모델 로드 성공: {}", modelName);
            return;
        } catch (Exception e) {
            log.warn("HuggingFace 모델 로드 실패, 로컬 모델 시도: {}", e.getMessage());
        }

        try {
            Criteria<String, ai.djl.modality.Classifications> criteria = Criteria.builder()
                    .optApplication(Application.NLP.SENTIMENT_ANALYSIS)
                    .setTypes(String.class, ai.djl.modality.Classifications.class)
                    .optFilter("backbone", "distilbert")
                    .optEngine("PyTorch")
                    .build();

            model = criteria.loadModel();
            log.info("로컬 DistilBERT 모델 로드 성공");
        } catch (Exception e) {
            log.error("모든 모델 로드 방법 실패: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public DjlSentimentResult analyzeSentiment(String text) {
        if (!modelAvailable || model == null) {
            return createFallbackResult(text, "모델을 사용할 수 없습니다");
        }

        long startTime = System.currentTimeMillis();

        try (Predictor<String, ai.djl.modality.Classifications> predictor = model.newPredictor()) {
            ai.djl.modality.Classifications result = predictor.predict(text);

            String topClass = result.best().getClassName();
            double confidence = result.best().getProbability();

            return DjlSentimentResult.builder()
                    .label(mapLabelToStandard(topClass))
                    .score(confidence)
                    .confidence(confidence)
                    .success(true)
                    .modelName(modelName)
                    .originalText(text)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

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
        return texts.stream()
                .map(this::analyzeSentiment)
                .toList();
    }

    @Override
    public DjlSentimentResult analyzeNewsContent(Content content) {
        if (content == null) {
            return createFallbackResult("", "콘텐츠가 null입니다");
        }

        String combinedText = content.getTitle() + ". " + content.getContent();
        return analyzeSentiment(combinedText);
    }

    @Override
    public boolean isModelAvailable() {
        return modelAvailable && model != null;
    }

    @Override
    public List<String> getAvailableModels() {
        return Arrays.asList(
                "cardiffnlp/twitter-roberta-base-sentiment-latest",
                "distilbert-base-uncased-finetuned-sst-2-english"
        );
    }

    @Override
    public Map<String, Object> getModelMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelName", modelName);
        metadata.put("isAvailable", isModelAvailable());
        metadata.put("enabled", djlEnabled);
        return metadata;
    }

    private String mapLabelToStandard(String originalLabel) {
        return switch (originalLabel.toUpperCase()) {
            case "NEGATIVE", "NEG", "0" -> "NEGATIVE";
            case "POSITIVE", "POS", "1" -> "POSITIVE";
            default -> "NEUTRAL";
        };
    }

    private DjlSentimentResult createFallbackResult(String text, String errorMessage) {
        return DjlSentimentResult.builder()
                .label("NEUTRAL")
                .score(0.5)
                .confidence(0.0)
                .success(false)
                .errorMessage(errorMessage)
                .modelName(modelName)
                .originalText(text)
                .processingTimeMs(0L)
                .build();
    }

    @PreDestroy
    public void cleanup() {
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