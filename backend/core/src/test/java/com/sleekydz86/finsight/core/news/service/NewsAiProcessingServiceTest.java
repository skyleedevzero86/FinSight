package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NewsAiProcessingServiceTest {

    private static final Executor DIRECT = command -> command.run();

    @Test
    @DisplayName("번역 결과를 원본 출처에 붙인다")
    void attachesTranslationToTheOriginalSource() {
        // given
        News source = sourceNews(9L);
        News analyzed = News.createWithoutAI(new NewsMeta(NewsProvider.ALL, LocalDateTime.now(), "AI Generated"), source.getOriginalContent())
                .updateAiAnalysis("금리 동결 요약", "연준이 금리를 동결했다", "금리를 유지했다.", List.of(TargetCategory.SPY), SentimentType.NEUTRAL, 0.4);
        News result = new NewsAiProcessingService(new FixedAnalysis(analyzed), DIRECT).processNewsWithAI(source);

        // when

        // then
        assertThat(result.getNewsMeta().getSourceUrl()).isEqualTo("https://example.com/fed");
        assertThat(result.getTranslatedContent().getTitle()).isEqualTo("연준이 금리를 동결했다");
        assertThat(result.getAiOverView().getOverview()).isEqualTo("금리 동결 요약");
    }

    @Test
    @DisplayName("분석이 없으면 원본을 유지한다")
    void keepsOriginalWhenAnalysisIsMissing() {
        // given
        News source = sourceNews(10L);
        News result = new NewsAiProcessingService(new FixedAnalysis(null), DIRECT).processNewsWithAI(source);

        // when

        // then
        assertThat(result.getTranslatedContent()).isNull();
        assertThat(result.getNewsMeta().getSourceUrl()).isEqualTo("https://example.com/fed");
    }

    @Test
    @DisplayName("여러 기사를 겹쳐서 분석하고 순서를 유지한다")
    void analyzesArticlesConcurrentlyAndKeepsOrder() throws Exception {
        // given
        OverlappingAi ai = new OverlappingAi();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // when

        // then
        try {
            NewsAiProcessingService service = new NewsAiProcessingService(ai, pool);
            List<News> analyzed = service.processNewsWithAI(List.of(
                    news("https://news.example/1", "첫째"),
                    news("https://news.example/2", "둘째")));

            assertThat(ai.overlapped).isTrue();
            assertThat(analyzed).extracting(item -> item.getOriginalContent().getTitle())
                    .containsExactly("첫째", "둘째");
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    private static News sourceNews(Long id) {
        return new News(
                id,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T01:00:00"),
                new Content("Fed holds", "Rates unchanged."),
                null,
                null,
                new NewsMeta(NewsProvider.MARKETAUX, LocalDateTime.parse("2026-10-06T22:00:00"), "https://example.com/fed"));
    }

    private static News news(String url, String title) {
        return News.createWithoutAI(
                NewsMeta.of(NewsProvider.YAHOO_FINANCE, url),
                new Content(title, "본문"));
    }

    private static final class FixedAnalysis implements NewsAiAnalysisRequesterPort {
        private final News analyzed;

        private FixedAnalysis(News analyzed) {
            this.analyzed = analyzed;
        }

        @Override
        public List<News> analyseNewses(AiModel model, Content content) {
            return analyzed == null ? List.of() : List.of(analyzed);
        }

        @Override
        public CompletableFuture<List<News>> analyseNewsesAsync(AiModel model, Content content) {
            return CompletableFuture.completedFuture(analyseNewses(model, content));
        }

        @Override
        public List<News> analyseNewsesBatch(AiModel model, List<Content> contents) {
            return analyseNewses(model, contents.isEmpty() ? null : contents.get(0));
        }

        @Override
        public boolean isModelAvailable(AiModel model) {
            return true;
        }

        @Override
        public List<AiModel> getAvailableModels() {
            return List.of(AiModel.CHATGPT);
        }
    }

    private static final class OverlappingAi implements NewsAiAnalysisRequesterPort {
        private final AtomicInteger inFlight = new AtomicInteger();
        private volatile boolean overlapped;
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public List<News> analyseNewses(AiModel model, Content content) {
            int current = inFlight.incrementAndGet();
            if (current > 1) {
                overlapped = true;
                release.countDown();
            }
            try {
                release.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                inFlight.decrementAndGet();
            }
            return List.of();
        }

        @Override
        public CompletableFuture<List<News>> analyseNewsesAsync(AiModel model, Content content) {
            return CompletableFuture.completedFuture(List.of());
        }

        @Override
        public List<News> analyseNewsesBatch(AiModel model, List<Content> contents) {
            return List.of();
        }

        @Override
        public boolean isModelAvailable(AiModel model) {
            return false;
        }

        @Override
        public List<AiModel> getAvailableModels() {
            return List.of();
        }
    }
}
