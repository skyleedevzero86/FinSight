package com.sleekydz86.finsight.core.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@ConditionalOnMissingBean(name = "redisTemplate")
public class FallbackCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(FallbackCacheConfig.class);

    @Bean
    @Primary
    public CacheManager fallbackCacheManager() {
        logger.info("RedisTemplate이 없어 Caffeine 로컬 캐시를 사용합니다");

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
                "news", "news_list", "news_search", "board", "board_list",
                "comment", "comment_list",
                "user", "userCache", "userProfile", "user_profile",
                "userList", "pendingUsers",
                "rate_limit", "session_cache", "api_cache"
        ));

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((key, value, cause) ->
                        logger.debug("캐시 제거: key={}, cause={}", key, cause)));

        return cacheManager;
    }
}