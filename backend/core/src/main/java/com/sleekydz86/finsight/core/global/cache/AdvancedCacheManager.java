package com.sleekydz86.finsight.core.global.cache;

import com.sleekydz86.finsight.core.global.annotation.Cacheable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AdvancedCacheManager {

    private final CacheManager cacheManager;
    private final ConcurrentMap<String, CacheStatistics> cacheStats = new ConcurrentHashMap<>();

    public AdvancedCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public <T> T get(String cacheName, String key, Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return null;
        }

        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper != null) {
            updateCacheStats(cacheName, true);
            return (T) wrapper.get();
        }

        updateCacheStats(cacheName, false);
        return null;
    }

    public void put(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
            updateCacheStats(cacheName, true);
        }
    }

    public void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    public void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    public void evictAll() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            evictAll(cacheName);
        }
    }

    public CacheStatistics getCacheStatistics(String cacheName) {
        return cacheStats.getOrDefault(cacheName, new CacheStatistics());
    }

    public Map<String, CacheStatistics> getAllCacheStatistics() {
        return new HashMap<>(cacheStats);
    }

    private void updateCacheStats(String cacheName, boolean hit) {
        cacheStats.computeIfAbsent(cacheName, k -> new CacheStatistics())
                .update(hit);
    }

    public static class CacheStatistics {
        private long hits = 0;
        private long misses = 0;
        private long totalRequests = 0;

        public void update(boolean hit) {
            totalRequests++;
            if (hit) {
                hits++;
            } else {
                misses++;
            }
        }

        public double getHitRate() {
            return totalRequests == 0 ? 0.0 : (double) hits / totalRequests;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public long getTotalRequests() {
            return totalRequests;
        }
    }
}