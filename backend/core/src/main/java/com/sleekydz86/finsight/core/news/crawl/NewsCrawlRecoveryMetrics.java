package com.sleekydz86.finsight.core.news.crawl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NewsCrawlRecoveryMetrics {

    private final AtomicInteger classified = new AtomicInteger();
    private final AtomicInteger persistFailures = new AtomicInteger();
    private final AtomicInteger unattendedAttempts = new AtomicInteger();
    private final AtomicInteger unattendedSuccess = new AtomicInteger();
    private final AtomicInteger approvedAttempts = new AtomicInteger();
    private final AtomicInteger approvedSuccess = new AtomicInteger();
    private final AtomicInteger humanApprovals = new AtomicInteger();
    private final AtomicInteger duplicateRequests = new AtomicInteger();
    private final AtomicInteger duplicateBlocked = new AtomicInteger();
    private final AtomicInteger redisFailures = new AtomicInteger();
    private final List<Long> approvedDurationsMs = new CopyOnWriteArrayList<>();
    private final List<String> persistFailureNotes = new CopyOnWriteArrayList<>();

    public void classified() {
        classified.incrementAndGet();
    }

    public void persistFailed(String note) {
        persistFailures.incrementAndGet();
        persistFailureNotes.add(note);
        while (persistFailureNotes.size() > 20) {
            persistFailureNotes.remove(0);
        }
    }

    public void approvedAttempt() {
        approvedAttempts.incrementAndGet();
    }

    public void approvedSucceeded(long durationMs) {
        approvedSuccess.incrementAndGet();
        approvedDurationsMs.add(durationMs);
    }

    public void approvedDuration(long durationMs) {
        approvedDurationsMs.add(durationMs);
    }

    public void humanApproval() {
        humanApprovals.incrementAndGet();
    }

    public void duplicateBlocked() {
        duplicateRequests.incrementAndGet();
        duplicateBlocked.incrementAndGet();
    }

    public void redisFailed() {
        redisFailures.incrementAndGet();
    }

    public int unattendedAttempts() {
        return unattendedAttempts.get();
    }

    public int unattendedSuccess() {
        return unattendedSuccess.get();
    }

    public Snapshot snapshot() {
        List<Long> durations = new ArrayList<>(approvedDurationsMs);
        return new Snapshot(
                classified.get(),
                persistFailures.get(),
                unattendedAttempts.get(),
                unattendedSuccess.get(),
                approvedAttempts.get(),
                approvedSuccess.get(),
                humanApprovals.get(),
                duplicateRequests.get(),
                duplicateBlocked.get(),
                redisFailures.get(),
                NewsCrawlLatency.mean(durations),
                NewsCrawlLatency.p95(durations),
                List.copyOf(persistFailureNotes));
    }

    public record Snapshot(
            int classified,
            int persistFailures,
            int unattendedAttempts,
            int unattendedSuccess,
            int approvedAttempts,
            int approvedSuccess,
            int humanApprovals,
            int duplicateRequests,
            int duplicateBlocked,
            int redisFailures,
            long meanRecoveryMs,
            long p95RecoveryMs,
            List<String> persistFailureNotes) {
    }
}
