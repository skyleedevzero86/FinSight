package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class NewsCrawlProviderGate implements NewsCrawlRunGate {

    private static final Logger log = LoggerFactory.getLogger(NewsCrawlProviderGate.class);
    private static final String KEY_PREFIX = "news-crawl:";

    private final NewsCrawlLockStore locks;
    private final NewsCrawlRedisLock redis;
    private final Clock clock;
    private final NewsCrawlRecoveryMetrics metrics;

    public NewsCrawlProviderGate(
            NewsCrawlLockStore locks,
            @Autowired(required = false) NewsCrawlRedisLock redis,
            Clock clock,
            NewsCrawlRecoveryMetrics metrics) {
        this.locks = locks;
        this.redis = redis;
        this.clock = clock;
        this.metrics = metrics;
    }

    @Override
    public Optional<NewsCrawlPermit> tryAcquire(NewsProvider provider, String owner, Duration ttl) {
        String key = KEY_PREFIX + provider.name();
        if (redisBlocks(key, owner, ttl)) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<Long> token = locks.acquire(provider, owner, now.plus(ttl), now);
        if (token.isEmpty()) {
            releaseRedis(key, owner);
            return Optional.empty();
        }
        return Optional.of(new NewsCrawlPermit(provider, owner, token.get()));
    }

    @Override
    public void release(NewsCrawlPermit permit) {
        if (permit == null) {
            return;
        }
        locks.release(permit.provider(), permit.owner(), permit.fencingToken());
        releaseRedis(KEY_PREFIX + permit.provider().name(), permit.owner());
    }

    @Override
    public boolean stillHeld(NewsCrawlPermit permit) {
        if (permit == null) {
            return false;
        }
        return locks.held(permit.provider(), permit.owner(), permit.fencingToken(), LocalDateTime.now(clock));
    }

    private boolean redisBlocks(String key, String owner, Duration ttl) {
        NewsCrawlRedisLock lock = redis;
        if (lock == null) {
            return false;
        }
        try {
            return !lock.tryHold(key, owner, ttl);
        } catch (RuntimeException error) {
            metrics.redisFailed();
            log.warn("Redis 수집 잠금에 실패해 DB 잠금만 사용합니다 key={}", key, error);
            return false;
        }
    }

    private void releaseRedis(String key, String owner) {
        if (redis == null) {
            return;
        }
        try {
            redis.release(key, owner);
        } catch (RuntimeException error) {
            metrics.redisFailed();
            log.warn("Redis 수집 잠금 해제에 실패했습니다 key={}", key, error);
        }
    }
}
