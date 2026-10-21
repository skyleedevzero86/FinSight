package com.sleekydz86.finsight.core.news.crawl;

import java.util.ArrayList;
import java.util.List;

public final class NewsCrawlLatency {

    private NewsCrawlLatency() {
    }

    public static boolean lockSkipOnly(int ranProviders, int lockSkips) {
        return ranProviders == 0 && lockSkips > 0;
    }

    public static long mean(List<Long> samples) {
        if (samples == null || samples.isEmpty()) {
            return 0L;
        }
        long sum = 0L;
        for (Long sample : samples) {
            sum += sample;
        }
        return sum / samples.size();
    }

    public static long p95(List<Long> samples) {
        if (samples == null || samples.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(samples);
        sorted.sort(Long::compareTo);
        int index = (int) Math.ceil(sorted.size() * 0.95d) - 1;
        return sorted.get(Math.max(index, 0));
    }
}
