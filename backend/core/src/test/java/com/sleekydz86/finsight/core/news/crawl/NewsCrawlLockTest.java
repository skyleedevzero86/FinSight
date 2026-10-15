package com.sleekydz86.finsight.core.news.crawl;

import com.sleekydz86.finsight.core.global.NewsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NewsCrawlLockTest {

    @Test
    void 잠금_소유자와_만료와_펜싱_토큰을_지킨다() {
        MemoryLockStore locks = new MemoryLockStore();
        MutableClock clock = new MutableClock(Instant.parse("2026-10-15T00:00:00Z"));
        NewsCrawlProviderGate gate = gate(locks, null, clock, new NewsCrawlRecoveryMetrics());

        Optional<NewsCrawlPermit> first = gate.tryAcquire(NewsProvider.YAHOO_FINANCE, "batch-1", Duration.ofSeconds(1));
        Optional<NewsCrawlPermit> blocked = gate.tryAcquire(NewsProvider.YAHOO_FINANCE, "manual-1", Duration.ofSeconds(1));

        assertThat(first).isPresent();
        assertThat(blocked).isEmpty();
        assertThat(gate.stillHeld(first.get())).isTrue();
        assertThat(locks.release(NewsProvider.YAHOO_FINANCE, "manual-1", first.get().fencingToken())).isFalse();

        clock.plusSeconds(2);
        assertThat(gate.stillHeld(first.get())).isFalse();
        Optional<NewsCrawlPermit> stolen = gate.tryAcquire(NewsProvider.YAHOO_FINANCE, "agent-1", Duration.ofSeconds(1));
        assertThat(stolen).isPresent();
        assertThat(stolen.get().fencingToken()).isNotEqualTo(first.get().fencingToken());
        assertThat(locks.release(NewsProvider.YAHOO_FINANCE, first.get().owner(), first.get().fencingToken())).isFalse();
        assertThat(gate.stillHeld(stolen.get())).isTrue();
    }

    @Test
    void Redis가_죽어도_DB_잠금으로_중복_실행을_막는다() {
        MemoryLockStore locks = new MemoryLockStore();
        NewsCrawlRecoveryMetrics metrics = new NewsCrawlRecoveryMetrics();
        NewsCrawlProviderGate gate = gate(locks, new DownRedis(), Clock.systemUTC(), metrics);

        assertThat(gate.tryAcquire(NewsProvider.MARKETAUX, "batch-1", Duration.ofMinutes(2))).isPresent();
        assertThat(gate.tryAcquire(NewsProvider.MARKETAUX, "manual-1", Duration.ofMinutes(2))).isEmpty();
        assertThat(metrics.snapshot().redisFailures()).isGreaterThan(0);
    }

    @Test
    void 잠금만_건너뛴_실행은_성공이_아니다() {
        assertThat(NewsCrawlLatency.lockSkipOnly(0, 3)).isTrue();
        assertThat(NewsCrawlLatency.lockSkipOnly(1, 2)).isFalse();
        assertThat(NewsCrawlLatency.lockSkipOnly(0, 0)).isFalse();
    }

    private static NewsCrawlProviderGate gate(
            NewsCrawlLockStore locks,
            NewsCrawlRedisLock redis,
            Clock clock,
            NewsCrawlRecoveryMetrics metrics) {
        return new NewsCrawlProviderGate(locks, redis, clock, metrics);
    }

    private static final class DownRedis implements NewsCrawlRedisLock {
        @Override
        public boolean tryHold(String key, String owner, Duration ttl) {
            throw new RedisConnectionFailureException("Redis 연결 실패");
        }

        @Override
        public void release(String key, String owner) {
            throw new RedisConnectionFailureException("Redis 연결 실패");
        }
    }

    static final class MemoryLockStore implements NewsCrawlLockStore {
        private final Map<NewsProvider, Row> rows = new HashMap<>();

        @Override
        public synchronized Optional<Long> acquire(
                NewsProvider provider, String owner, LocalDateTime expiresAt, LocalDateTime now) {
            Row row = rows.get(provider);
            if (row != null && row.expires.isAfter(now) && !row.owner.equals(owner)) {
                return Optional.empty();
            }
            long token = row == null || !row.expires.isAfter(now) ? (row == null ? 1L : row.token + 1) : row.token;
            rows.put(provider, new Row(owner, token, expiresAt));
            return Optional.of(token);
        }

        @Override
        public synchronized boolean release(NewsProvider provider, String owner, long fencingToken) {
            Row row = rows.get(provider);
            if (row == null || !row.owner.equals(owner) || row.token != fencingToken) {
                return false;
            }
            row.expires = LocalDateTime.MIN;
            return true;
        }

        @Override
        public synchronized boolean held(NewsProvider provider, String owner, long fencingToken, LocalDateTime now) {
            Row row = rows.get(provider);
            return row != null && row.owner.equals(owner) && row.token == fencingToken && row.expires.isAfter(now);
        }

        private static final class Row {
            private String owner;
            private long token;
            private LocalDateTime expires;

            private Row(String owner, long token, LocalDateTime expires) {
                this.owner = owner;
                this.token = token;
                this.expires = expires;
            }
        }
    }

    static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void plusSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
