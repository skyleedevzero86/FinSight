package com.sleekydz86.finsight.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnBean(name = "redisTemplate")
public class RedisHealthService {

    private static final Logger logger = LoggerFactory.getLogger(RedisHealthService.class);

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public boolean isRedisAvailable() {
        if (redisTemplate == null) {
            logger.warn("Redis 템플릿이 사용할 수 없습니다");
            return false;
        }

        try {
            String testKey = "health:check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "test", 10, TimeUnit.SECONDS);
            String value = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);

            boolean isAvailable = "test".equals(value);
            logger.info("Redis 연결 상태: {}", isAvailable ? "정상" : "비정상");
            return isAvailable;

        } catch (Exception e) {
            logger.error("Redis 연결 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    public void logRedisStatus() {
        if (isRedisAvailable()) {
            logger.info("Redis 서비스가 정상적으로 작동 중입니다");
        } else {
            logger.warn("Redis 서비스에 연결할 수 없습니다. 대체 캐시를 사용합니다");
        }
    }
}