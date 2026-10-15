package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.adapter.requester.NewsScrapRequester;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.Newses;
import com.sleekydz86.finsight.core.news.domain.port.out.NewsPersistencePort;
import com.sleekydz86.finsight.core.news.service.NewsAiProcessingService;
import com.sleekydz86.finsight.core.news.service.NewsNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class NewsProviderRecrawl {

    private static final Logger log = LoggerFactory.getLogger(NewsProviderRecrawl.class);
    private static final Duration LOCK_TTL = Duration.ofMinutes(2);

    private final Map<NewsProvider, NewsScrapRequester> requesters;
    private final NewsPersistencePort persistence;
    private final NewsAiProcessingService aiProcessing;
    private final NewsNotificationService notifications;
    private final NewsCrawlIncidentService incidents;
    private final NewsCrawlRunGate gate;
    private final NewsCrawlRecoveryMetrics metrics;
    private final Clock clock;

    public NewsProviderRecrawl(
            List<NewsScrapRequester> requesters,
            NewsPersistencePort persistence,
            NewsAiProcessingService aiProcessing,
            NewsNotificationService notifications,
            NewsCrawlIncidentService incidents,
            NewsCrawlRunGate gate,
            NewsCrawlRecoveryMetrics metrics,
            Clock clock) {
        this.requesters = requesters.stream()
                .collect(Collectors.toMap(NewsScrapRequester::supports, requester -> requester, (left, right) -> left));
        this.persistence = persistence;
        this.aiProcessing = aiProcessing;
        this.notifications = notifications;
        this.incidents = incidents;
        this.gate = gate;
        this.metrics = metrics;
        this.clock = clock;
    }

    public NewsCrawlReport executeApproved(long incidentId, String executor) {
        NewsCrawlIncident current = incidents.get(incidentId);
        if (current.status() == NewsCrawlIncidentStatus.SUCCEEDED || current.status() == NewsCrawlIncidentStatus.RUNNING) {
            metrics.duplicateBlocked();
            return incidents.report(current, latest(incidentId), "이미 실행 중이거나 끝난 사건이라 다시 실행하지 않았습니다");
        }
        if (current.status() != NewsCrawlIncidentStatus.APPROVED) {
            throw new IllegalStateException("승인된 사건만 재처리할 수 있습니다. incidentId=" + incidentId);
        }
        Optional<NewsCrawlIncident> claimed = incidents.claimRunning(incidentId);
        if (claimed.isEmpty()) {
            metrics.duplicateBlocked();
            return incidents.report(incidents.get(incidentId), latest(incidentId), "다른 실행이 사건을 선점해 재수집하지 않았습니다");
        }
        metrics.approvedAttempt();
        return runClaimed(claimed.get(), executor);
    }

    private NewsCrawlReport runClaimed(NewsCrawlIncident incident, String executor) {
        long started = System.nanoTime();
        LocalDateTime startedAt = LocalDateTime.now(clock);
        Optional<NewsCrawlPermit> permit = gate.tryAcquire(incident.provider(), "incident-" + incident.id(), LOCK_TTL);
        if (permit.isEmpty()) {
            incidents.revertToApproved(incident.id());
            return skipped(incident.id(), executor, startedAt, started, "수집 잠금 충돌로 재수집을 실행하지 않았습니다");
        }
        try {
            return scrapeAndSave(incident, executor, startedAt, started, permit.get());
        } finally {
            gate.release(permit.get());
        }
    }

    private NewsCrawlReport scrapeAndSave(
            NewsCrawlIncident incident,
            String executor,
            LocalDateTime startedAt,
            long started,
            NewsCrawlPermit permit) {
        try {
            List<News> scraped = scrap(incident.provider());
            if (!gate.stillHeld(permit)) {
                return fail(incident.id(), executor, startedAt, started, 0, 0, "잠금이 만료되어 저장을 중단했습니다");
            }
            List<News> fresh = persistence.omitStoredDuplicates(scraped);
            int duplicates = scraped.size() - fresh.size();
            List<News> analyzed = fresh.isEmpty() ? List.of() : aiProcessing.processNewsWithAI(fresh);
            Newses saved = analyzed.isEmpty() ? new Newses() : persistence.saveAllNews(analyzed);
            if (saved.getNewses().size() != analyzed.size()) {
                return fail(incident.id(), executor, startedAt, started, saved.getNewses().size(), duplicates, "재처리 저장 건수가 일치하지 않습니다");
            }
            if (!saved.getNewses().isEmpty()) {
                notifications.sendNotificationsForImportantNews(saved.getNewses());
            }
            return succeed(incident.id(), executor, startedAt, started, saved.getNewses().size(), duplicates);
        } catch (DataIntegrityViolationException error) {
            log.warn("뉴스 중복 URL 제약으로 재수집 저장을 거부했습니다 incidentId={}", incident.id(), error);
            return fail(incident.id(), executor, startedAt, started, 0, 0, "이미 있는 뉴스 URL이라 저장하지 않았습니다");
        } catch (RuntimeException error) {
            log.error("뉴스 재수집에 실패했습니다 incidentId={}", incident.id(), error);
            return fail(incident.id(), executor, startedAt, started, 0, 0, NewsCrawlIncidentService.safeMessage(error));
        }
    }

    private List<News> scrap(NewsProvider provider) {
        NewsScrapRequester requester = requesters.get(provider);
        if (requester == null) {
            throw new IllegalStateException("재수집 요청자를 찾지 못했습니다. provider=" + provider);
        }
        try {
            return requester.scrap(LocalDateTime.now(clock).minusHours(24), 100).join();
        } catch (CompletionException error) {
            Throwable cause = error.getCause() == null ? error : error.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("제공처 재수집에 실패했습니다. provider=" + provider, cause);
        }
    }

    private NewsCrawlReport succeed(
            long incidentId, String executor, LocalDateTime startedAt, long started, int saved, int duplicates) {
        metrics.approvedSucceeded(millis(started));
        NewsCrawlIncident finished = incidents.finish(incidentId, NewsCrawlIncidentStatus.SUCCEEDED);
        NewsCrawlAttempt attempt = incidents.addAttempt(attempt(
                incidentId, executor, startedAt, NewsCrawlAttemptStatus.SUCCEEDED, saved, duplicates, "재수집 저장을 확인했습니다"));
        return incidents.report(finished, attempt, attempt.resultMessage());
    }

    private NewsCrawlReport fail(
            long incidentId, String executor, LocalDateTime startedAt, long started, int saved, int duplicates, String result) {
        metrics.approvedDuration(millis(started));
        NewsCrawlIncident finished = incidents.finish(incidentId, NewsCrawlIncidentStatus.FAILED);
        NewsCrawlAttempt attempt = incidents.addAttempt(attempt(
                incidentId, executor, startedAt, NewsCrawlAttemptStatus.FAILED, saved, duplicates, result));
        return incidents.report(finished, attempt, result);
    }

    private NewsCrawlReport skipped(long incidentId, String executor, LocalDateTime startedAt, long started, String result) {
        metrics.approvedDuration(millis(started));
        NewsCrawlAttempt attempt = incidents.addAttempt(attempt(
                incidentId, executor, startedAt, NewsCrawlAttemptStatus.SKIPPED, 0, 0, result));
        return incidents.report(incidents.get(incidentId), attempt, result);
    }

    private NewsCrawlAttempt attempt(
            long incidentId,
            String executor,
            LocalDateTime startedAt,
            NewsCrawlAttemptStatus status,
            int saved,
            int duplicates,
            String result) {
        return new NewsCrawlAttempt(
                null,
                incidentId,
                status,
                executor == null ? "" : executor,
                startedAt,
                LocalDateTime.now(clock),
                saved,
                duplicates,
                clip(result));
    }

    private NewsCrawlAttempt latest(long incidentId) {
        List<NewsCrawlAttempt> attempts = incidents.attempts(incidentId);
        return attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
    }

    private static long millis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private static String clip(String result) {
        if (result == null) {
            return "";
        }
        return result.length() <= 500 ? result : result.substring(0, 500);
    }
}
