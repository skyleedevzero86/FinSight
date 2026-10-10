package com.sleekydz86.finsight.core.search.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisSearchCache {

    private static final Logger log = LoggerFactory.getLogger(RedisSearchCache.class);
    private static final Duration TTL = Duration.ofSeconds(30);
    private static final String TIMING = "search:timing";

    private final ObjectProvider<RedisTemplate<String, String>> redis;

    public RedisSearchCache(@Qualifier("customStringRedisTemplate") ObjectProvider<RedisTemplate<String, String>> redis) {
        this.redis = redis;
    }

    public Optional<String> read(String key) {
        RedisTemplate<String, String> template = redis.getIfAvailable();
        if (template == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(template.opsForValue().get(key));
        } catch (RuntimeException exception) {
            log.warn("Redis 검색 캐시를 읽지 못했습니다. key={}", key, exception);
            return Optional.empty();
        }
    }

    public void write(String key, String value) {
        RedisTemplate<String, String> template = redis.getIfAvailable();
        if (template == null) {
            return;
        }
        try {
            template.opsForValue().set(key, value, TTL);
        } catch (RuntimeException exception) {
            log.warn("Redis 검색 캐시를 쓰지 못했습니다. key={}", key, exception);
        }
    }

    public void record(long tookMs, String engine) {
        RedisTemplate<String, String> template = redis.getIfAvailable();
        if (template == null) {
            return;
        }
        try {
            template.opsForList().leftPush(TIMING, tookMs + " " + engine);
            template.opsForList().trim(TIMING, 0, 49);
        } catch (RuntimeException exception) {
            log.warn("Redis 검색 시간을 남기지 못했습니다. tookMs={} engine={}", tookMs, engine, exception);
        }
    }
}
