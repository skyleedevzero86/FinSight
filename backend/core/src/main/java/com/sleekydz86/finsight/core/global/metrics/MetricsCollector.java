package com.sleekydz86.finsight.core.global.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsCollector {

    @Autowired
    private MeterRegistry meterRegistry;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

    public void incrementCounter(String name, String... tags) {
        Counter counter = counters.computeIfAbsent(name, k ->
                Counter.builder(name)
                        .description("Counter for " + name)
                        .register(meterRegistry));

        if (tags.length > 0) {
            Counter.builder(name)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        } else {
            counter.increment();
        }
    }

    public void recordTimer(String name, long duration, TimeUnit unit, String... tags) {
        Timer timer = timers.computeIfAbsent(name, k ->
                Timer.builder(name)
                        .description("Timer for " + name)
                        .register(meterRegistry));

        if (tags.length > 0) {
            Timer.builder(name)
                    .tags(tags)
                    .register(meterRegistry)
                    .record(duration, unit);
        } else {
            timer.record(duration, unit);
        }
    }

    public void recordGauge(String name, double value, String... tags) {
        meterRegistry.gauge(name, value);
    }

    public void recordBusinessMetric(String operation, String status, long duration) {
        incrementCounter("business.operation", "operation", operation, "status", status);
        recordTimer("business.operation.duration", duration, TimeUnit.MILLISECONDS, "operation", operation);
    }

    public void recordApiCall(String endpoint, String method, int statusCode, long duration) {
        incrementCounter("api.calls",
                "endpoint", endpoint,
                "method", method,
                "status", String.valueOf(statusCode));
        recordTimer("api.duration", duration, TimeUnit.MILLISECONDS,
                "endpoint", endpoint,
                "method", method);
    }

    public void recordDatabaseOperation(String operation, String table, long duration) {
        incrementCounter("database.operations",
                "operation", operation,
                "table", table);
        recordTimer("database.duration", duration, TimeUnit.MILLISECONDS,
                "operation", operation,
                "table", table);
    }

    public void recordCacheOperation(String operation, String cacheName, boolean hit) {
        incrementCounter("cache.operations",
                "operation", operation,
                "cache", cacheName,
                "hit", String.valueOf(hit));
    }

    public void recordError(String component, String errorType, String errorCode) {
        incrementCounter("errors",
                "component", component,
                "type", errorType,
                "code", errorCode);
    }
}