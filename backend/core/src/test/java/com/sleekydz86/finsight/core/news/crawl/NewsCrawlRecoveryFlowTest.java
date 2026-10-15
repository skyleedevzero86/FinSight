package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.AiModel;
import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.global.exception.InsufficientPermissionException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsCrawlRecoveryFlowTest {

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 승인된_재수집만_저장하고_중복_실행은_막는다() throws Exception {
        Harness harness = harness(Clock.systemUTC(), narrativeOf("규칙"));
        long incidentId = openIncident(harness, NewsProvider.MARKETAUX, http(500));
        admin();
        NewsCrawlReport analysis = harness.agent.analyze(incidentId);
        assertThat(analysis.approvalRequired()).isTrue();
        assertThat(analysis.result()).contains("승인");
        assertThat(harness.calls().get()).isZero();

        harness.agent.approve(incidentId, "admin@test");
        NewsCrawlReport first = harness.agent.execute(incidentId, "admin@test");
        assertThat(first.incidentStatus()).isEqualTo(NewsCrawlIncidentStatus.SUCCEEDED);
        assertThat(first.savedCount()).isEqualTo(1);
        assertThat(harness.persistence.saved).hasSize(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(() -> harness.recrawl.executeApproved(incidentId, "again-1")).get(5, TimeUnit.SECONDS);
            pool.submit(() -> harness.recrawl.executeApproved(incidentId, "again-2")).get(5, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        harness.recrawl.executeApproved(incidentId, "again-3");
        harness.recrawl.executeApproved(incidentId, "again-4");

        assertThat(harness.calls().get()).isEqualTo(1);
        NewsCrawlRecoveryMetrics.Snapshot snapshot = harness.metrics.snapshot();
        int blocked = snapshot.duplicateBlocked();
        int requests = snapshot.duplicateRequests();
        System.out.println("중복 실행 방지 " + blocked + "/" + requests);
        System.out.println("사람 승인 " + snapshot.humanApprovals() + " 무인시도 " + snapshot.unattendedAttempts());
        assertThat(requests).isEqualTo(4);
        assertThat(blocked).isEqualTo(4);
        assertThat(snapshot.unattendedAttempts()).isZero();
        assertThat(snapshot.humanApprovals()).isEqualTo(1);
    }

    @Test
    void 승인_없이_실행하거나_권한이_없으면_재수집하지_않는다() {
        Harness harness = harness(Clock.systemUTC(), narrativeOf("규칙"));
        long incidentId = openIncident(harness, NewsProvider.YAHOO_FINANCE, new java.net.ConnectException("refused"));
        admin();
        assertThatThrownBy(() -> harness.agent.execute(incidentId, "admin@test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("승인된 사건만");
        user();
        assertThatThrownBy(() -> harness.agent.execute(incidentId, "user@test"))
                .isInstanceOfAny(InsufficientPermissionException.class, ResponseStatusException.class);
        assertThat(harness.calls().get()).isZero();
    }

    @Test
    void 호출_제한과_권한_오류는_정책이_재수집을_거절한다() {
        Harness harness = harness(Clock.systemUTC(), narrativeOf("규칙"));
        long limited = openIncident(harness, NewsProvider.ALPHA_VANTAGE, http(429, "30"));
        long denied = openIncident(harness, NewsProvider.ALPHA_VANTAGE, http(401, null));
        admin();
        harness.agent.analyze(limited);
        harness.agent.analyze(denied);
        assertThatThrownBy(() -> harness.agent.approve(limited, "admin@test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재처리 정책");
        assertThatThrownBy(() -> harness.agent.approve(denied, "admin@test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재처리 정책");
        assertThat(harness.calls().get()).isZero();
    }

    @Test
    void 모델이_시간_초과되면_규칙_분류만_남기고_배치_기록은_유지된다() {
        Harness harness = harness(Clock.systemUTC(), incident -> {
            throw new TimeoutException("모델 응답 시간이 초과되었습니다");
        });
        long incidentId = openIncident(harness, NewsProvider.MARKETAUX, http(503));
        NewsCrawlReport report = harness.agent.analyze(incidentId);
        assertThat(report.modelNote()).contains("규칙 분류만");
        assertThat(report.incidentStatus()).isEqualTo(NewsCrawlIncidentStatus.ANALYZED);
        assertThat(harness.incidents.get(incidentId).cause()).isEqualTo(NewsCrawlCause.HTTP_5XX);
        assertThat(harness.calls().get()).isZero();
    }

    @Test
    void 사건_저장이_실패해도_관측_기록은_남는다() {
        Harness harness = harness(Clock.systemUTC(), narrativeOf("규칙"));
        harness.store.failInsert = true;
        harness.incidents.record(NewsProvider.YAHOO_FINANCE, http(500));
        NewsCrawlRecoveryMetrics.Snapshot snapshot = harness.metrics.snapshot();
        assertThat(snapshot.persistFailures()).isEqualTo(1);
        assertThat(snapshot.persistFailureNotes()).isNotEmpty();
        assertThat(harness.store.rows).isEmpty();
    }

    @Test
    void 저장_불일치와_중복_URL은_성공이_아니다() {
        Harness mismatch = harness(Clock.systemUTC(), narrativeOf("규칙"));
        mismatch.persistence.mismatch = true;
        long mismatchId = approved(mismatch, NewsProvider.MARKETAUX, http(500));
        NewsCrawlReport mismatchReport = mismatch.recrawl.executeApproved(mismatchId, "admin@test");
        assertThat(mismatchReport.incidentStatus()).isEqualTo(NewsCrawlIncidentStatus.FAILED);
        assertThat(mismatchReport.result()).contains("일치하지");

        Harness duplicate = harness(Clock.systemUTC(), narrativeOf("규칙"));
        duplicate.persistence.duplicateUrl = true;
        long duplicateId = approved(duplicate, NewsProvider.YAHOO_FINANCE, http(500));
        NewsCrawlReport duplicateReport = duplicate.recrawl.executeApproved(duplicateId, "admin@test");
        assertThat(duplicateReport.incidentStatus()).isEqualTo(NewsCrawlIncidentStatus.FAILED);
        assertThat(duplicateReport.result()).contains("URL");
        assertThat(duplicate.persistence.saved).isEmpty();
    }

    @Test
    void 잠금이_만료되면_저장하지_않는다() {
        Harness harness = harness(Clock.systemUTC(), narrativeOf("규칙"));
        harness.gate().held = false;
        long incidentId = approved(harness, NewsProvider.MARKETAUX, http(500));
        NewsCrawlReport report = harness.recrawl.executeApproved(incidentId, "admin@test");
        assertThat(report.incidentStatus()).isEqualTo(NewsCrawlIncidentStatus.FAILED);
        assertThat(report.result()).contains("잠금이 만료");
        assertThat(harness.persistence.saved).isEmpty();
    }

    @Test
    void 빈_응답은_장애_사건이_아니고_승인_복구_시간을_측정한다() {
        Harness harness = harness(Clock.systemUTC(), narrativeOf("규칙"));
        harness.rows().clear();
        List<Long> samples = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            long started = System.nanoTime();
            long incidentId = approved(harness, NewsProvider.YAHOO_FINANCE, http(500));
            NewsCrawlReport report = harness.recrawl.executeApproved(incidentId, "admin@test");
            samples.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            assertThat(report.incidentStatus()).isEqualTo(NewsCrawlIncidentStatus.SUCCEEDED);
            assertThat(report.savedCount()).isZero();
        }
        long mean = NewsCrawlLatency.mean(samples);
        long p95 = NewsCrawlLatency.p95(samples);
        NewsCrawlRecoveryMetrics.Snapshot snapshot = harness.metrics.snapshot();
        System.out.println("승인 복구 테스트 평균ms " + mean + " p95ms " + p95);
        System.out.println("승인 복구 성공 " + snapshot.approvedSuccess() + "/" + snapshot.approvedAttempts());
        assertThat(snapshot.approvedAttempts()).isEqualTo(20);
        assertThat(snapshot.approvedSuccess()).isEqualTo(20);
        assertThat(snapshot.unattendedAttempts()).isZero();
        assertThat(p95).isGreaterThanOrEqualTo(mean);
        assertThat(List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)).isNotEmpty();
        assertThat(NewsCrawlLatency.mean(List.of(10L, 20L, 30L))).isEqualTo(20L);
        assertThat(NewsCrawlLatency.p95(List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L))).isEqualTo(100L);
    }

    private static long approved(Harness harness, NewsProvider provider, Throwable error) {
        long incidentId = openIncident(harness, provider, error);
        admin();
        harness.agent.analyze(incidentId);
        harness.agent.approve(incidentId, "admin@test");
        return incidentId;
    }

    private static long openIncident(Harness harness, NewsProvider provider, Throwable error) {
        boolean previous = harness.store.failInsert;
        harness.store.failInsert = false;
        harness.incidents.record(provider, error);
        harness.store.failInsert = previous;
        return harness.store.latestId;
    }

    private static Harness harness(Clock clock, NewsCrawlNarrative narrative) {
        MemoryIncidentStore store = new MemoryIncidentStore();
        NewsCrawlRecoveryMetrics metrics = new NewsCrawlRecoveryMetrics();
        NewsCrawlIncidentService incidents = new NewsCrawlIncidentService(store, clock, metrics);
        AtomicInteger calls = new AtomicInteger();
        List<News> rows = new ArrayList<>(List.of(News.createWithoutAI(
                NewsMeta.of(NewsProvider.MARKETAUX, "https://news.example/recovery"),
                new Content("복구 뉴스", "본문"))));
        CountingRequester requester = new CountingRequester(NewsProvider.MARKETAUX, calls, rows);
        CountingRequester yahoo = new CountingRequester(NewsProvider.YAHOO_FINANCE, calls, rows);
        CountingRequester alpha = new CountingRequester(NewsProvider.ALPHA_VANTAGE, calls, rows);
        FlagPersistence persistence = new FlagPersistence();
        NewsCrawlRunGate gate = new ToggleGate();
        NewsProviderRecrawl recrawl = new NewsProviderRecrawl(
                List.of(requester, yahoo, alpha),
                persistence,
                new NewsAiProcessingService(new EmptyAi(), new DirectExecutor()),
                new NewsNotificationService(null),
                incidents,
                gate,
                metrics,
                clock);
        NewsCrawlRecoveryAgent agent = new NewsCrawlRecoveryAgent(incidents, recrawl, narrative);
        return new Harness(store, incidents, metrics, calls, rows, persistence, (ToggleGate) gate, recrawl, agent);
    }

    private static NewsCrawlNarrative narrativeOf(String note) {
        return incident -> note;
    }

    private static WebClientResponseException http(int status) {
        return http(status, null);
    }

    private static WebClientResponseException http(int status, String retryAfter) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (retryAfter != null) {
            headers.set(org.springframework.http.HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return WebClientResponseException.create(status, "status", headers, new byte[0], StandardCharsets.UTF_8);
    }

    private static void admin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@test", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private static void user() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@test", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private record Harness(
            MemoryIncidentStore store,
            NewsCrawlIncidentService incidents,
            NewsCrawlRecoveryMetrics metrics,
            AtomicInteger calls,
            List<News> rows,
            FlagPersistence persistence,
            ToggleGate gate,
            NewsProviderRecrawl recrawl,
            NewsCrawlRecoveryAgent agent) {
    }

    private static final class ToggleGate implements NewsCrawlRunGate {
        private boolean held = true;

        @Override
        public Optional<NewsCrawlPermit> tryAcquire(NewsProvider provider, String owner, Duration ttl) {
            return Optional.of(new NewsCrawlPermit(provider, owner, 1L));
        }

        @Override
        public void release(NewsCrawlPermit permit) {
        }

        @Override
        public boolean stillHeld(NewsCrawlPermit permit) {
            return held;
        }
    }

    private static final class CountingRequester implements NewsScrapRequester {
        private final NewsProvider provider;
        private final AtomicInteger calls;
        private final List<News> rows;

        private CountingRequester(NewsProvider provider, AtomicInteger calls, List<News> rows) {
            this.provider = provider;
            this.calls = calls;
            this.rows = rows;
        }

        @Override
        public NewsProvider supports() {
            return provider;
        }

        @Override
        public CompletableFuture<List<News>> scrap(LocalDateTime publishTimeAfter, int limit) {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture(List.copyOf(rows));
        }
    }

    private static final class FlagPersistence implements NewsPersistencePort {
        private final List<News> saved = new ArrayList<>();
        private boolean mismatch;
        private boolean duplicateUrl;

        @Override
        public Newses saveAllNews(List<News> newses) {
            if (duplicateUrl) {
                throw new DataIntegrityViolationException("uk_news_source_url");
            }
            saved.addAll(newses);
            return mismatch ? new Newses() : new Newses(newses);
        }

        @Override
        public List<News> omitStoredDuplicates(List<News> newses) {
            return newses;
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

    private static final class EmptyAi implements NewsAiAnalysisRequesterPort {
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

    private static final class DirectExecutor implements Executor {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static final class MemoryIncidentStore implements NewsCrawlIncidentStore {
        private final HashMap<Long, NewsCrawlIncident> rows = new HashMap<>();
        private final HashMap<Long, List<NewsCrawlAttempt>> attempts = new HashMap<>();
        private long sequence = 1L;
        private long attemptSequence = 1L;
        private long latestId;
        private boolean failInsert;

        @Override
        public synchronized NewsCrawlIncident insert(NewsCrawlIncident incident) {
            if (failInsert) {
                throw new IllegalStateException("사건 저장 실패");
            }
            long id = sequence++;
            latestId = id;
            NewsCrawlIncident saved = new NewsCrawlIncident(
                    id,
                    incident.provider(),
                    incident.status(),
                    incident.cause(),
                    incident.httpStatus(),
                    incident.retryAfterSeconds(),
                    incident.message(),
                    incident.retryable(),
                    incident.occurredAt(),
                    incident.analyzedAt(),
                    incident.modelNote(),
                    incident.approvedBy(),
                    incident.approvedAt(),
                    incident.fencingToken());
            rows.put(id, saved);
            return saved;
        }

        @Override
        public synchronized Optional<NewsCrawlIncident> findById(long id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public synchronized List<NewsCrawlIncident> findRecent(int limit) {
            return rows.values().stream().limit(limit).toList();
        }

        @Override
        public synchronized Optional<NewsCrawlIncident> updateIfStatus(
                long id, NewsCrawlIncidentStatus expected, NewsCrawlIncident replacement) {
            NewsCrawlIncident current = rows.get(id);
            if (current == null || current.status() != expected) {
                return Optional.empty();
            }
            NewsCrawlIncident saved = new NewsCrawlIncident(
                    id,
                    replacement.provider(),
                    replacement.status(),
                    replacement.cause(),
                    replacement.httpStatus(),
                    replacement.retryAfterSeconds(),
                    replacement.message(),
                    replacement.retryable(),
                    replacement.occurredAt(),
                    replacement.analyzedAt(),
                    replacement.modelNote(),
                    replacement.approvedBy(),
                    replacement.approvedAt(),
                    replacement.fencingToken());
            rows.put(id, saved);
            return Optional.of(saved);
        }

        @Override
        public synchronized NewsCrawlAttempt insertAttempt(NewsCrawlAttempt attempt) {
            NewsCrawlAttempt saved = new NewsCrawlAttempt(
                    attemptSequence++,
                    attempt.incidentId(),
                    attempt.status(),
                    attempt.executor(),
                    attempt.startedAt(),
                    attempt.finishedAt(),
                    attempt.savedCount(),
                    attempt.duplicateCount(),
                    attempt.resultMessage());
            attempts.computeIfAbsent(attempt.incidentId(), key -> new ArrayList<>()).add(saved);
            return saved;
        }

        @Override
        public synchronized List<NewsCrawlAttempt> findAttempts(long incidentId) {
            return List.copyOf(attempts.getOrDefault(incidentId, List.of()));
        }
    }
}
