package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NewsCrawlIncidentService implements NewsCrawlFailureSink {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlIncidentService.class);
    private static final int MESSAGE_LIMIT = 500;
    private static final int NOTE_LIMIT = 1000;

    private final NewsCrawlIncidentStore store;
    private final Clock clock;
    private final NewsCrawlRecoveryMetrics metrics;

    public NewsCrawlIncidentService(NewsCrawlIncidentStore store, Clock clock, NewsCrawlRecoveryMetrics metrics) {
        this.store = store;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    public void record(NewsProvider provider, Throwable error) {
        NewsCrawlClassification classification = NewsCrawlCauseClassifier.classify(error);
        String message = clip(safeMessage(error), MESSAGE_LIMIT);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean retryable = NewsCrawlRetryPolicy.retryable(
                classification.cause(), classification.retryAfterSeconds(), now, now);
        log.error(
                "뉴스 수집 장애 관측 provider={} cause={} http={} message={}",
                provider,
                classification.cause(),
                classification.httpStatus(),
                message);
        try {
            store.insert(new NewsCrawlIncident(
                    null,
                    provider,
                    NewsCrawlIncidentStatus.OPEN,
                    classification.cause(),
                    classification.httpStatus(),
                    classification.retryAfterSeconds(),
                    message,
                    retryable,
                    now,
                    null,
                    null,
                    null,
                    null,
                    0L));
            metrics.classified();
        } catch (RuntimeException failure) {
            String note = provider + " " + classification.cause() + " 사건 저장 실패: " + failure.getClass().getSimpleName();
            metrics.persistFailed(note);
            log.error("뉴스 수집 장애 사건 저장 실패 provider={} cause={}", provider, classification.cause(), failure);
        }
    }

    public NewsCrawlIncident get(long id) {
        return store.findById(id).orElseThrow(() -> missing(id));
    }

    public List<NewsCrawlIncident> list(int limit) {
        return store.findRecent(Math.min(Math.max(limit, 1), 50));
    }

    public List<NewsCrawlAttempt> attempts(long incidentId) {
        get(incidentId);
        return store.findAttempts(incidentId);
    }

    public NewsCrawlIncident markAnalyzed(long id, String modelNote) {
        NewsCrawlIncident current = get(id);
        if (current.status() != NewsCrawlIncidentStatus.OPEN && current.status() != NewsCrawlIncidentStatus.ANALYZED) {
            throw illegal(id, current.status());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        boolean retryable = NewsCrawlRetryPolicy.retryable(
                current.cause(), current.retryAfterSeconds(), current.occurredAt(), now);
        NewsCrawlIncident next = current.copy(
                NewsCrawlIncidentStatus.ANALYZED,
                retryable,
                now,
                clip(modelNote, NOTE_LIMIT),
                current.approvedBy(),
                current.approvedAt(),
                current.fencingToken());
        return store.updateIfStatus(id, current.status(), next).orElseThrow(() -> illegal(id, current.status()));
    }

    public NewsCrawlIncident approve(long id, String approver) {
        NewsCrawlIncident current = get(id);
        if (current.status() != NewsCrawlIncidentStatus.ANALYZED) {
            throw illegal(id, current.status());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        boolean retryable = NewsCrawlRetryPolicy.retryable(
                current.cause(), current.retryAfterSeconds(), current.occurredAt(), now);
        if (!retryable) {
            throw new IllegalStateException("재처리 정책이 이 장애의 재수집을 허용하지 않습니다. incidentId=" + id);
        }
        NewsCrawlIncident next = current.copy(
                NewsCrawlIncidentStatus.APPROVED,
                true,
                current.analyzedAt(),
                current.modelNote(),
                clip(approver, 255),
                now,
                current.fencingToken());
        NewsCrawlIncident saved = store.updateIfStatus(id, NewsCrawlIncidentStatus.ANALYZED, next)
                .orElseThrow(() -> illegal(id, current.status()));
        metrics.humanApproval();
        return saved;
    }

    public Optional<NewsCrawlIncident> claimRunning(long id) {
        NewsCrawlIncident current = get(id);
        if (current.status() != NewsCrawlIncidentStatus.APPROVED) {
            return Optional.empty();
        }
        NewsCrawlIncident next = current.copy(
                NewsCrawlIncidentStatus.RUNNING,
                current.retryable(),
                current.analyzedAt(),
                current.modelNote(),
                current.approvedBy(),
                current.approvedAt(),
                current.fencingToken() + 1);
        return store.updateIfStatus(id, NewsCrawlIncidentStatus.APPROVED, next);
    }

    public NewsCrawlIncident finish(long id, NewsCrawlIncidentStatus status) {
        NewsCrawlIncident current = get(id);
        if (current.status() != NewsCrawlIncidentStatus.RUNNING) {
            throw illegal(id, current.status());
        }
        if (status != NewsCrawlIncidentStatus.SUCCEEDED && status != NewsCrawlIncidentStatus.FAILED) {
            throw illegal(id, current.status());
        }
        NewsCrawlIncident next = current.copy(
                status,
                current.retryable(),
                current.analyzedAt(),
                current.modelNote(),
                current.approvedBy(),
                current.approvedAt(),
                current.fencingToken());
        return store.updateIfStatus(id, NewsCrawlIncidentStatus.RUNNING, next).orElseThrow(() -> illegal(id, current.status()));
    }

    public void revertToApproved(long id) {
        NewsCrawlIncident current = get(id);
        if (current.status() != NewsCrawlIncidentStatus.RUNNING) {
            return;
        }
        NewsCrawlIncident next = current.copy(
                NewsCrawlIncidentStatus.APPROVED,
                current.retryable(),
                current.analyzedAt(),
                current.modelNote(),
                current.approvedBy(),
                current.approvedAt(),
                current.fencingToken());
        store.updateIfStatus(id, NewsCrawlIncidentStatus.RUNNING, next);
    }

    public NewsCrawlAttempt addAttempt(NewsCrawlAttempt attempt) {
        return store.insertAttempt(attempt);
    }

    public NewsCrawlReport report(NewsCrawlIncident incident, NewsCrawlAttempt attempt, String result) {
        return new NewsCrawlReport(
                incident.id(),
                incident.provider().name(),
                incident.status(),
                incident.cause(),
                incident.retryable(),
                incident.status() == NewsCrawlIncidentStatus.ANALYZED,
                incident.approvedBy(),
                incident.approvedAt(),
                attempt == null ? null : attempt.executor(),
                attempt == null ? null : attempt.startedAt(),
                attempt == null ? null : attempt.finishedAt(),
                result,
                attempt == null ? 0 : attempt.savedCount(),
                attempt == null ? 0 : attempt.duplicateCount(),
                incident.modelNote());
    }

    private static IllegalStateException missing(long id) {
        return new IllegalStateException("뉴스 수집 장애 사건을 찾지 못했습니다. incidentId=" + id);
    }

    private static IllegalStateException illegal(long id, NewsCrawlIncidentStatus status) {
        return new IllegalStateException("허용되지 않은 뉴스 수집 사건 상태입니다. incidentId=" + id + ", status=" + status);
    }

    static String safeMessage(Throwable error) {
        Throwable root = error == null ? null : rootOf(error);
        String raw = root == null ? "" : root.getClass().getSimpleName() + ": " + root.getMessage();
        return raw.replaceAll("(?i)(api_token|apikey|api_key)=[^&\\s]+", "$1=***");
    }

    private static Throwable rootOf(Throwable error) {
        Throwable root = error;
        int guard = 0;
        while (root.getCause() != null && root.getCause() != root && guard++ < 12) {
            root = root.getCause();
        }
        return root;
    }

    private static String clip(String value, int max) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
