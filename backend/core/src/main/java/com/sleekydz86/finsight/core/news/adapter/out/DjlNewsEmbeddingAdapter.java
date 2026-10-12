package com.sleekydz86.finsight.core.news.adapter.out;

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsEmbeddingPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DjlNewsEmbeddingAdapter implements NewsEmbeddingPort {

    private static final Logger log = LoggerFactory.getLogger(DjlNewsEmbeddingAdapter.class);
    private static final int MAX_CHARS = 1200;

    @Value("${ai.djl.embedding.enabled:false}")
    private boolean enabled;

    @Value("${ai.djl.embedding.model:sentence-transformers/all-mpnet-base-v2}")
    private String modelName;

    @Value("${ai.djl.embedding.dims:768}")
    private int dimensions;

    private ZooModel<String, float[]> model;
    private DjlThreadPredictor<String, float[]> predictors;

    @PostConstruct
    public void initialize() {
        if (!enabled) {
            log.info("뉴스 임베딩 모델이 비활성화되었습니다.");
            return;
        }
        try {
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/" + modelName)
                    .optEngine("PyTorch")
                    .optOption("translatorFactory", TextEmbeddingTranslatorFactory.class.getName())
                    .build();
            model = criteria.loadModel();
            predictors = new DjlThreadPredictor<>(model);
            log.info("뉴스 임베딩 모델 로드 완료: {} dims={}", modelName, dimensions);
        } catch (Exception exception) {
            log.error("뉴스 임베딩 모델 로드 실패. model={}", modelName, exception);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isReady() {
        return enabled && model != null && predictors != null;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(String text) {
        String clipped = clip(text);
        if (!isReady() || clipped.isEmpty()) {
            return null;
        }
        try {
            float[] vector = predictors.borrow().predict(clipped);
            if (vector == null || vector.length != dimensions) {
                int actual = vector == null ? 0 : vector.length;
                log.error("뉴스 임베딩 차원이 설정과 다릅니다. 기대={} 실제={} model={}", dimensions, actual, modelName);
                return null;
            }
            return vector;
        } catch (Exception exception) {
            log.error("뉴스 임베딩 생성 실패. model={}", modelName, exception);
            return null;
        }
    }

    private String clip(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= MAX_CHARS ? trimmed : trimmed.substring(0, MAX_CHARS);
    }

    @PreDestroy
    public void cleanup() {
        if (predictors != null) {
            predictors.close();
        }
        if (model != null) {
            model.close();
        }
    }
}
