package com.sleekydz86.finsight.batch.news.scrap.tasklet;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.service.NewsAiProcessingService;
import com.sleekydz86.finsight.core.news.service.NewsNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class NewsCrawlingTaskletTest {

    @Test
    void 한_제공처가_실패해도_성공한_뉴스를_저장한다() throws Exception {
        RecordingNewsPersistence persistence = new RecordingNewsPersistence();
        NewsCrawlingTasklet tasklet = new NewsCrawlingTasklet(
                List.of(successRequester(), failingRequester()),
                persistence,
                new NewsAiProcessingService(new EmptyAiAnalysis()),
                new NewsNotificationService(null));

        RepeatStatus status = tasklet.execute(contribution(), chunkContext());

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        assertThat(persistence.saved).extracting(news -> news.getOriginalContent().getTitle())
                .containsExactly("저장된 뉴스");
        assertThat(tasklet.getCrawlingMetrics().errorCount()).containsEntry(NewsProvider.MARKETAUX, 1);
        assertThat(tasklet.getCrawlingMetrics().scrapedNewsCount()).containsEntry(NewsProvider.YAHOO_FINANCE, 1);
    }

    private static NewsScrapRequester successRequester() {
        News news = News.createWithoutAI(
                NewsMeta.of(NewsProvider.YAHOO_FINANCE, "https://news.example/ok"),
                new Content("저장된 뉴스", "본문"));
        return new FixedRequester(NewsProvider.YAHOO_FINANCE, CompletableFuture.completedFuture(List.of(news)));
    }

    private static NewsScrapRequester failingRequester() {
        return new FixedRequester(
                NewsProvider.MARKETAUX,
                CompletableFuture.failedFuture(new IllegalStateException("수집 실패")));
    }

    private static StepContribution contribution() {
        StepExecution stepExecution = new StepExecution("newsCrawlingStep", new JobExecution(1L, new JobParameters()));
        return new StepContribution(stepExecution);
    }

    private static ChunkContext chunkContext() {
        StepExecution stepExecution = new StepExecution("newsCrawlingStep", new JobExecution(2L, new JobParameters()));
        return new ChunkContext(new StepContext(stepExecution));
    }

    private static final class FixedRequester implements NewsScrapRequester {
        private final NewsProvider provider;
        private final CompletableFuture<List<News>> future;

        private FixedRequester(NewsProvider provider, CompletableFuture<List<News>> future) {
            this.provider = provider;
            this.future = future;
        }

        @Override
        public NewsProvider supports() {
            return provider;
        }

        @Override
        public CompletableFuture<List<News>> scrap(LocalDateTime publishTimeAfter, int limit) {
            return future;
        }
    }

    private static final class EmptyAiAnalysis implements NewsAiAnalysisRequesterPort {
        @Override
        public List<News> analyseNewses(AiModel model, Content content) {
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

    private static final class RecordingNewsPersistence implements NewsPersistencePort {
        private final List<News> saved = new ArrayList<>();

        @Override
        public Newses saveAllNews(List<News> newses) {
            saved.addAll(newses);
            return new Newses(newses);
        }

        @Override
        public Newses findByOverviewIsNull() {
            return new Newses();
        }

        @Override
        public Newses findAllByFilters(NewsQueryRequest request) {
            return new Newses();
        }

        @Override
        public Optional<News> findById(Long newsId) {
            return Optional.empty();
        }

        @Override
        public Newses searchByQuery(NewsSearchRequest request) {
            return new Newses();
        }

        @Override
        public Newses findByCategory(String category, int limit) {
            return new Newses();
        }

        @Override
        public Newses findPopularNews(int limit) {
            return new Newses();
        }

        @Override
        public Newses findLatestNews(int limit) {
            return new Newses();
        }

        @Override
        public Newses findLatestNewsByProvider(NewsProvider provider, int limit) {
            return new Newses();
        }

        @Override
        public Newses findRelatedNews(Long newsId, List<String> categories, int limit) {
            return new Newses();
        }

        @Override
        public Newses findByIds(List<Long> ids) {
            return new Newses();
        }
    }
}
