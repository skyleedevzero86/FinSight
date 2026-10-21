package com.sleekydz86.finsight.core.news.adapter.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlAttempt;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlIncident;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlIncidentStatus;
import com.sleekydz86.finsight.core.news.crawl.NewsCrawlIncidentStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class NewsCrawlIncidentJpaStore implements NewsCrawlIncidentStore {

    private final NewsCrawlIncidentJpaRepository incidents;
    private final NewsCrawlAttemptJpaRepository attempts;

    public NewsCrawlIncidentJpaStore(
            NewsCrawlIncidentJpaRepository incidents,
            NewsCrawlAttemptJpaRepository attempts) {
        this.incidents = incidents;
        this.attempts = attempts;
    }

    @Override
    @Transactional
    public NewsCrawlIncident insert(NewsCrawlIncident incident) {
        NewsCrawlIncidentJpaEntity entity = new NewsCrawlIncidentJpaEntity();
        write(entity, incident);
        return toIncident(incidents.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NewsCrawlIncident> findById(long id) {
        return incidents.findById(id).map(NewsCrawlIncidentJpaStore::toIncident);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsCrawlIncident> findRecent(int limit) {
        return incidents.findTop50ByOrderByIdDesc().stream()
                .limit(limit)
                .map(NewsCrawlIncidentJpaStore::toIncident)
                .toList();
    }

    @Override
    @Transactional
    public Optional<NewsCrawlIncident> updateIfStatus(
            long id,
            NewsCrawlIncidentStatus expected,
            NewsCrawlIncident replacement) {
        NewsCrawlIncidentJpaEntity entity = incidents.lockById(id).orElse(null);
        if (entity == null || entity.getStatus() != expected) {
            return Optional.empty();
        }
        write(entity, replacement);
        entity.setId(id);
        return Optional.of(toIncident(entity));
    }

    @Override
    @Transactional
    public NewsCrawlAttempt insertAttempt(NewsCrawlAttempt attempt) {
        NewsCrawlAttemptJpaEntity entity = new NewsCrawlAttemptJpaEntity();
        entity.setIncidentId(attempt.incidentId());
        entity.setStatus(attempt.status());
        entity.setExecutor(attempt.executor());
        entity.setStartedAt(attempt.startedAt());
        entity.setFinishedAt(attempt.finishedAt());
        entity.setSavedCount(attempt.savedCount());
        entity.setDuplicateCount(attempt.duplicateCount());
        entity.setResultMessage(attempt.resultMessage());
        NewsCrawlAttemptJpaEntity saved = attempts.save(entity);
        return toAttempt(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NewsCrawlAttempt> findAttempts(long incidentId) {
        return attempts.findByIncidentIdOrderByIdAsc(incidentId).stream()
                .map(NewsCrawlIncidentJpaStore::toAttempt)
                .toList();
    }

    private static void write(NewsCrawlIncidentJpaEntity entity, NewsCrawlIncident incident) {
        entity.setProvider(incident.provider().name());
        entity.setStatus(incident.status());
        entity.setCause(incident.cause());
        entity.setHttpStatus(incident.httpStatus());
        entity.setRetryAfterSeconds(incident.retryAfterSeconds());
        entity.setMessage(incident.message());
        entity.setRetryable(incident.retryable());
        entity.setOccurredAt(incident.occurredAt());
        entity.setAnalyzedAt(incident.analyzedAt());
        entity.setModelNote(incident.modelNote());
        entity.setApprovedBy(blankToNull(incident.approvedBy()));
        entity.setApprovedAt(incident.approvedAt());
        entity.setFencingToken(incident.fencingToken());
    }

    private static NewsCrawlIncident toIncident(NewsCrawlIncidentJpaEntity entity) {
        return new NewsCrawlIncident(
                entity.getId(),
                NewsProvider.valueOf(entity.getProvider()),
                entity.getStatus(),
                entity.getCause(),
                entity.getHttpStatus(),
                entity.getRetryAfterSeconds(),
                entity.getMessage(),
                entity.isRetryable(),
                entity.getOccurredAt(),
                entity.getAnalyzedAt(),
                entity.getModelNote(),
                entity.getApprovedBy(),
                entity.getApprovedAt(),
                entity.getFencingToken());
    }

    private static NewsCrawlAttempt toAttempt(NewsCrawlAttemptJpaEntity entity) {
        return new NewsCrawlAttempt(
                entity.getId(),
                entity.getIncidentId(),
                entity.getStatus(),
                entity.getExecutor(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getSavedCount(),
                entity.getDuplicateCount(),
                entity.getResultMessage());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
