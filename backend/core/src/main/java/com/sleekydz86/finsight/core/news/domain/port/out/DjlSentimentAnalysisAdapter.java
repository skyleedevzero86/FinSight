package com.sleekydz86.finsight.core.news.domain.port.out;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.sleekydz86.finsight.core.news.domain.port.out.DjlSentimentAnalysisPort;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class DjlSentimentAnalysisAdapter implements DjlSentimentAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(DjlSentimentAnalysisAdapter.class);

    @Value("${ai.djl.model.name:cardiffnlp/twitter-roberta-base-sentiment-latest}")
    private String modelName;

    @Value("${ai.djl.model.type:sentiment}")
    private String modelType;

    @Value("${ai.djl.model.max-length:512}")
    private int maxLength;

    @Value("${ai.djl.model.batch-size:32}")
    private int batchSize;

    @Value("${ai.djl.model.timeout:30000}")
    private long timeout;

    private ZooModel<String, float[]> model;
    private final Map<String, String[]> labelMappings = new ConcurrentHashMap<>();
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    private static final String[] DEFAULT_LABELS = {"Negative", "Neutral", "Positive"};
    private static final Map<String, String[]> MODEL_LABELS = Map.of(
            "cardiffnlp/twitter-roberta-base-sentiment-latest", new String[]{"Negative", "Neutral", "Positive"},
            "cardiffnlp/twitter-roberta-base-sentiment", new String[]{"Negative", "Neutral", "Positive"},
            "nlptown/bert-base-multilingual-uncased-sentiment", new String[]{"1 star", "2 stars", "3 stars", "4 stars", "5 stars"},
            "finiteautomata/bertweet-base-sentiment-analysis", new String[]{"NEG", "NEU", "POS"}
    );

    @PostConstruct
    public void initialize() {
        try {
            loadModel();
            log.info("DJL 감정분석 모델 로드 완료: {}", modelName);
        } catch (Exception e) {
            log.error("DJL 감정분석 모델 로드 실패: {}", e.getMessage(), e);
        }
    }

    private void loadModel() throws ModelNotFoundException, MalformedModelException, IOException {
        Criteria<String, float[]> criteria = Criteria.builder()
                .optApplication(Application.NLP.SENTIMENT_ANALYSIS)
                .setTypes(String.class, float[].class)
                .optModelUrls("https://huggingface.co/" + modelName)
                .optEngine("PyTorch")
                .optProgress(new ai.djl.training.util.ProgressBar())
                .build();

        model = criteria.loadModel();

        // 라벨 매핑 설정
        String[] labels = MODEL_LABELS.getOrDefault(modelName, DEFAULT_LABELS);
        labelMappings.put(modelName, labels);

        log.info("모델 {} 로드 완료, 라벨: {}", modelName, Arrays.toString(labels));
    }

    @Override
    public DjlSentimentResult analyzeSentiment(String text) {
        long startTime = System.currentTimeMillis();
        totalRequests.incrementAndGet();

        try {
            if (model == null) {
                throw new IllegalStateException("모델이 로드되지 않았습니다");
            }

            String processedText = preprocessText(text);

            try (Predictor<String, float[]> predictor = model.newPredictor()) {
                float[] logits = predictor.predict(processedText);

                DjlSentimentResult result = processResults(logits, processedText);
                result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

                successfulRequests.incrementAndGet();
                totalProcessingTime.addAndGet(result.getProcessingTimeMs());

                return result;
            }

        } catch (Exception e) {
            failedRequests.incrementAndGet();
            log.error("감정분석 실패: {}", e.getMessage(), e);

            return DjlSentimentResult.builder()
                    .label("NEUTRAL")
                    .score(0.0)
                    .confidence(0.0)
                    .errorMessage(e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public CompletableFuture<DjlSentimentResult> analyzeSentimentAsync(String text) {
        return CompletableFuture.supplyAsync(() -> analyzeSentiment(text));
    }

    @Override
    public List<DjlSentimentResult> analyzeSentimentBatch(List<String> texts) {
        List<DjlSentimentResult> results = new ArrayList<>();

        for (String text : texts) {
            try {
                DjlSentimentResult result = analyzeSentiment(text);
                results.add(result);
            } catch (Exception e) {
                log.error("배치 처리 중 오류 발생: {}", e.getMessage());
                results.add(DjlSentimentResult.builder()
                        .label("NEUTRAL")
                        .score(0.0)
                        .confidence(0.0)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return results;
    }

    @Override
    public DjlSentimentResult analyzeNewsContent(Content content) {
        if (content == null) {
            return DjlSentimentResult.builder()
                    .label("NEUTRAL")
                    .score(0.0)
                    .confidence(0.0)
                    .errorMessage("콘텐츠가 null입니다")
                    .build();
        }

        // 제목과 내용을 결합하여 분석
        String combinedText = content.getTitle() + ". " + content.getContent();
        return analyzeSentiment(combinedText);
    }

    @Override
    public boolean isModelAvailable() {
        return model != null;
    }

    @Override
    public List<String> getAvailableModels() {
        return new ArrayList<>(MODEL_LABELS.keySet());
    }

    @Override
    public Map<String, Object> getModelMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("modelName", modelName);
        metadata.put("modelType", modelType);
        metadata.put("maxLength", maxLength);
        metadata.put("batchSize", batchSize);
        metadata.put("timeout", timeout);
        metadata.put("isAvailable", isModelAvailable());
        metadata.put("totalRequests", totalRequests.get());
        metadata.put("successfulRequests", successfulRequests.get());
        metadata.put("failedRequests", failedRequests.get());
        metadata.put("totalProcessingTime", totalProcessingTime.get());

        if (totalRequests.get() > 0) {
            metadata.put("successRate", (double) successfulRequests.get() / totalRequests.get());
            metadata.put("averageProcessingTime", (double) totalProcessingTime.get() / successfulRequests.get());
        }

        return metadata;
    }

    private String preprocessText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // 텍스트 길이 제한
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }

        // 특수 문자 처리
        text = text.replaceAll("@\\w+", "@user")
                .replaceAll("http[s]?://\\S+", "http")
                .replaceAll("[\\r\\n\\t]+", " ")
                .trim();

        return text;
    }

    private DjlSentimentResult processResults(float[] logits, String processedText) {
        String[] labels = labelMappings.getOrDefault(modelName, DEFAULT_LABELS);

        // 소프트맥스 적용
        double[] probabilities = softmax(logits);

        // 최고 점수 찾기
        int maxIndex = 0;
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > probabilities[maxIndex]) {
                maxIndex = i;
            }
        }

        // 모든 점수 매핑
        Map<String, Double> allScores = new HashMap<>();
        for (int i = 0; i < labels.length && i < probabilities.length; i++) {
            allScores.put(labels[i], probabilities[i]);
        }

        return DjlSentimentResult.builder()
                .label(labels[maxIndex])
                .score(probabilities[maxIndex])
                .confidence(probabilities[maxIndex])
                .allScores(allScores)
                .tokens(Arrays.asList(processedText.split("\\s+")))
                .modelName(modelName)
                .build();
    }

    private double[] softmax(float[] logits) {
        double[] exp = new double[logits.length];
        double sum = 0.0;

        double max = Arrays.stream(logits).mapToDouble(f -> f).max().orElse(0);

        for (int i = 0; i < logits.length; i++) {
            exp[i] = Math.exp(logits[i] - max);
            sum += exp[i];
        }

        for (int i = 0; i < exp.length; i++) {
            exp[i] /= sum;
        }

        return exp;
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