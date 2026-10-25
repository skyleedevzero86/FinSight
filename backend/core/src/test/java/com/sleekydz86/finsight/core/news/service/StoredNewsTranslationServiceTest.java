package com.sleekydz86.finsight.core.news.service;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsDetailResponse;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsQueryRequest;
import com.sleekydz86.finsight.core.news.domain.port.in.dto.NewsSearchRequest;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsAiAnalysisRequesterPort;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.domain.vo.Content;
import com.sleekydz86.finsight.core.news.domain.vo.NewsMeta;
import com.sleekydz86.finsight.core.news.domain.vo.SentimentType;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class StoredNewsTranslationServiceTest {

    private static final Executor DIRECT = command -> command.run();

    @Test
    @DisplayName("번역 제목을 저장하고 같은 기사는 다시 맡기지 않는다")
    void savesTranslatedTitleAndSkipsTheSameArticle() {
        // given
        MemoryNews news = new MemoryNews();
        CountingAi ai = new CountingAi("연준이 금리를 유지했다");
        StoredNewsTranslationService service = service(ai, news);
        News stored = english(4L);

        // when
        service.fill(new Newses(List.of(stored)));
        service.fill(new Newses(news.saved));

        // then
        assertThat(ai.calls()).isEqualTo(1);
        assertThat(news.saved).hasSize(1);
        assertThat(news.saved.get(0).getTranslatedContent().getTitle()).isEqualTo("연준이 금리를 유지했다");
        assertThat(news.saved.get(0).getId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("번역이 원문과 같으면 저장하지 않고 다시 맡기지 않는다")
    void skipsSaveWhenTranslationMatchesOriginal() {
        // given
        MemoryNews news = new MemoryNews();
        CountingAi ai = new CountingAi("Fed holds rates");
        StoredNewsTranslationService service = service(ai, news);

        // when
        service.fill(new Newses(List.of(english(5L))));
        service.fill(new Newses(List.of(english(5L))));

        // then
        assertThat(ai.calls()).isEqualTo(1);
        assertThat(news.saved).isEmpty();
    }

    @Test
    @DisplayName("한 번에 맡기는 기사는 열두 건을 넘지 않는다")
    void translatesAtMostTwelveArticlesPerRequest() {
        // given
        MemoryNews news = new MemoryNews();
        CountingAi ai = new CountingAi("한글 제목");
        List<News> rows = new ArrayList<>();
        for (long id = 1; id <= 13; id++) {
            rows.add(english(id));
        }

        // when
        service(ai, news).fill(new Newses(rows));

        // then
        assertThat(ai.calls()).isEqualTo(StoredNewsTranslationService.MAX_PER_REQUEST);
        assertThat(news.saved).hasSize(StoredNewsTranslationService.MAX_PER_REQUEST);
    }

    @Test
    @DisplayName("상세는 번역이 없는 기사만 맡긴다")
    void translatesDetailOnlyWhenTitleIsMissing() {
        // given
        MemoryNews news = new MemoryNews();
        News stored = english(7L);
        news.rows.add(stored);
        CountingAi ai = new CountingAi("연준이 금리를 유지했다");
        StoredNewsTranslationService service = service(ai, news);
        NewsDetailResponse detail = NewsDetailResponse.builder()
                .id(7L)
                .originalTitle("Fed holds rates")
                .translatedTitle("Fed holds rates")
                .build();

        // when
        NewsDetailResponse filled = service.fill(detail);
        service.fill(detail);

        // then
        assertThat(filled.getOriginalTitle()).isEqualTo("Fed holds rates");
        assertThat(ai.calls()).isEqualTo(1);
        assertThat(news.saved).hasSize(1);
    }

    @Test
    @DisplayName("한글이 아닌 다른 영어 제목은 저장하지 않는다")
    void doesNotSaveEnglishParaphrase() {
        // given
        MemoryNews news = new MemoryNews();
        CountingAi ai = new CountingAi("The Fed left rates unchanged");

        // when
        service(ai, news).fill(new Newses(List.of(english(6L))));

        // then
        assertThat(news.saved).isEmpty();
        assertThat(StoredNewsTranslationService.readableKorean("객개객개객개객개", "Fed holds rates")).isFalse();
        assertThat(StoredNewsTranslationService.readableKorean("연준이 금리를 유지했다", "Fed holds rates")).isTrue();
    }

    @Test
    @DisplayName("조회는 감정을 계산하지 않고 번역만 맡긴다")
    void leavesSentimentForTheBatch() {
        // given
        MemoryNews news = new MemoryNews();
        CountingAi ai = new CountingAi("연준이 금리를 유지했다");

        // when
        Newses filled = service(ai, news).fill(new Newses(List.of(english(8L))));

        // then
        assertThat(filled.getNewses().get(0).getAiOverView()).isNull();
        assertThat(news.saved).hasSize(1);
        assertThat(news.saved.get(0).getTranslatedContent().getTitle()).isEqualTo("연준이 금리를 유지했다");
    }

    @Test
    @DisplayName("번역은 받은 실행기에만 맡긴다")
    void queuesTranslationOnTheGivenExecutor() {
        // given
        MemoryNews news = new MemoryNews();
        CountingAi ai = new CountingAi("연준이 금리를 유지했다");
        List<Runnable> queued = new ArrayList<>();
        StoredNewsTranslationService service = new StoredNewsTranslationService(
                new NewsAiProcessingService(ai, DIRECT), news, queued::add);

        // when
        service.fill(new Newses(List.of(english(11L))));

        // then
        assertThat(ai.calls()).isZero();
        assertThat(queued).hasSize(1);
        queued.get(0).run();
        assertThat(ai.calls()).isEqualTo(1);
    }

    private static StoredNewsTranslationService service(CountingAi ai, MemoryNews news) {
        return new StoredNewsTranslationService(
                new NewsAiProcessingService(ai, DIRECT),
                news,
                DIRECT);
    }

    private static News english(Long id) {
        return new News(
                id,
                NewsProvider.YAHOO_FINANCE,
                LocalDateTime.parse("2026-10-07T01:00:00"),
                new Content("Fed holds rates", "Rates unchanged."),
                null,
                null,
                new NewsMeta(NewsProvider.YAHOO_FINANCE, LocalDateTime.parse("2026-10-06T22:00:00"), "https://example.com/" + id));
    }

    private static final class CountingAi implements NewsAiAnalysisRequesterPort {
        private final String title;
        private final AtomicInteger calls = new AtomicInteger();

        private CountingAi(String title) {
            this.title = title;
        }

        private int calls() {
            return calls.get();
        }

        @Override
        public List<News> analyseNewses(AiModel model, Content content) {
            calls.incrementAndGet();
            News analyzed = News.createWithoutAI(
                    new NewsMeta(NewsProvider.YAHOO_FINANCE, LocalDateTime.now(), "https://example.com/ai"),
                    new Content(title, "본문"));
            return List.of(analyzed.updateAiAnalysis("요약", title, "한글 본문", List.of(TargetCategory.SPY), SentimentType.NEUTRAL, 0.2));
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

    private static final class MemoryNews implements NewsPersistencePort {
        private final List<News> saved = new ArrayList<>();
        private final List<News> rows = new ArrayList<>();

        @Override
        public void saveStoredAnalysis(News news) {
            saved.add(news);
            rows.removeIf(row -> news.getId().equals(row.getId()));
            rows.add(news);
        }

        @Override
        public Optional<News> findById(Long newsId) {
            return rows.stream().filter(news -> newsId.equals(news.getId())).findFirst();
        }

        @Override
        public Newses saveAllNews(List<News> newses) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<News> omitStoredDuplicates(List<News> newses) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findByOverviewIsNull() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findAllByFilters(NewsQueryRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses searchByQuery(NewsSearchRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findByCategory(String category, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findPopularNews(int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findLatestNews(int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findLatestNewsByProvider(NewsProvider provider, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findRelatedNews(Long newsId, List<String> categories, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Newses findByIds(List<Long> ids) {
            throw new UnsupportedOperationException();
        }
    }
}
