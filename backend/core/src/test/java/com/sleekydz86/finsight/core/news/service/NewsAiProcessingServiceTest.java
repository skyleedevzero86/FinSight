package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NewsAiProcessingServiceTest {

    @Test
    void 여러_기사를_겹쳐서_분석하고_순서를_유지한다() throws Exception {
        OverlappingAi ai = new OverlappingAi();
        ExecutorService pool = Executors.newFixedThreadPool(2);
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

    private static News news(String url, String title) {
        return News.createWithoutAI(
                NewsMeta.of(NewsProvider.YAHOO_FINANCE, url),
                new Content(title, "본문"));
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
