package com.sleekydz86.finsight.core.news.adapter.crawl;

import com.sleekydz86.finsight.core.news.crawl.NewsCrawlRedisLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnBean(name = "customStringRedisTemplate")
public class RedisNewsCrawlLock implements NewsCrawlRedisLock {

    private static final DefaultRedisScript<Long> UNLOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final RedisTemplate<String, String> redis;

    public RedisNewsCrawlLock(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryHold(String key, String owner, Duration ttl) {
        Boolean held = redis.opsForValue().setIfAbsent(key, owner, ttl);
        return Boolean.TRUE.equals(held);
    }

    @Override
    public void release(String key, String owner) {
        redis.execute(UNLOCK, List.of(key), owner);
    }
}
