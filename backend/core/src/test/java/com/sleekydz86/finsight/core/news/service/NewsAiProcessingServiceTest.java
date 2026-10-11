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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class NewsAiProcessingServiceTest {

    @Test
    void 번역_결과를_원본_출처에_붙인다() {
        News source = new News(
                9L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T01:00:00"),
                new Content("Fed holds", "Rates unchanged."),
                null,
                null,
                new NewsMeta(NewsProvider.MARKETAUX, LocalDateTime.parse("2026-10-06T22:00:00"), "https://example.com/fed"));
        News analyzed = News.createWithoutAI(new NewsMeta(NewsProvider.ALL, LocalDateTime.now(), "AI Generated"), source.getOriginalContent())
                .updateAiAnalysis("금리 동결 요약", "연준이 금리를 동결했다", "금리를 유지했다.", List.of(TargetCategory.SPY), SentimentType.NEUTRAL, 0.4);
        News result = new NewsAiProcessingService(new FixedAnalysis(analyzed)).processNewsWithAI(source);
        assertThat(result.getNewsMeta().getSourceUrl()).isEqualTo("https://example.com/fed");
        assertThat(result.getTranslatedContent().getTitle()).isEqualTo("연준이 금리를 동결했다");
        assertThat(result.getAiOverView().getOverview()).isEqualTo("금리 동결 요약");
    }

    @Test
    void 분석이_없으면_원본을_유지한다() {
        News source = new News(
                10L,
                NewsProvider.MARKETAUX,
                LocalDateTime.parse("2026-10-07T01:00:00"),
                new Content("Fed holds", "Rates unchanged."),
                null,
                null,
                new NewsMeta(NewsProvider.MARKETAUX, LocalDateTime.parse("2026-10-06T22:00:00"), "https://example.com/fed"));
        News result = new NewsAiProcessingService(new FixedAnalysis(null)).processNewsWithAI(source);
        assertThat(result.getTranslatedContent()).isNull();
        assertThat(result.getNewsMeta().getSourceUrl()).isEqualTo("https://example.com/fed");
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
}
