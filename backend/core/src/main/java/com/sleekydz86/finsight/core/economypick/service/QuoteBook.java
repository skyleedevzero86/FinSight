package com.sleekydz86.finsight.core.economypick.service;

import com.sleekydz86.finsight.core.economypick.adapter.YahooChartClient;
import com.sleekydz86.finsight.core.economypick.domain.PriceBar;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class QuoteBook {

    private static final Duration TTL = Duration.ofMinutes(30);

    private final YahooChartClient yahooChartClient;
    private final Map<String, Hit> cache = new ConcurrentHashMap<>();

    public QuoteBook(YahooChartClient yahooChartClient) {
        this.yahooChartClient = yahooChartClient;
    }

    public Map<String, List<PriceBar>> load(Collection<String> symbols) {
        Map<String, List<PriceBar>> found = new ConcurrentHashMap<>();
        List<String> missing = missing(symbols, found);
        if (!missing.isEmpty()) {
            fetch(missing, found);
        }
        return found;
    }

    public List<PriceBar> bars(Map<String, List<PriceBar>> book, String symbol) {
        List<PriceBar> bars = book.get(symbol);
        return bars == null ? List.of() : bars;
    }

    private List<String> missing(Collection<String> symbols, Map<String, List<PriceBar>> found) {
        List<String> missing = new ArrayList<>();
        for (String symbol : symbols) {
            Hit hit = cache.get(symbol);
            if (hit != null && hit.fresh()) {
                found.put(symbol, hit.bars());
            } else {
                missing.add(symbol);
            }
        }
        return missing;
    }

    private void fetch(List<String> symbols, Map<String, List<PriceBar>> found) {
        Semaphore gate = new Semaphore(4);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> jobs = new ArrayList<>();
            for (String symbol : symbols) {
                jobs.add(executor.submit(() -> store(symbol, gate, found)));
            }
            await(jobs);
        }
    }

    private void store(String symbol, Semaphore gate, Map<String, List<PriceBar>> found) {
        gate.acquireUninterruptibly();
        try {
            List<PriceBar> bars = yahooChartClient.daily(symbol);
            if (!bars.isEmpty()) {
                cache.put(symbol, new Hit(List.copyOf(bars), Instant.now()));
                found.put(symbol, bars);
            }
        } finally {
            gate.release();
        }
    }

    private void await(List<Future<?>> jobs) {
        for (Future<?> job : jobs) {
            try {
                job.get(25, TimeUnit.SECONDS);
            } catch (Exception exception) {
                job.cancel(true);
            }
        }
    }

    private record Hit(List<PriceBar> bars, Instant storedAt) {
        private boolean fresh() {
            return storedAt.plus(TTL).isAfter(Instant.now());
        }
    }
}
