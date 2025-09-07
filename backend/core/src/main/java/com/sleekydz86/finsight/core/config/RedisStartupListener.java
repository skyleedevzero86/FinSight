package com.sleekydz86.finsight.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RedisStartupListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisStartupListener.class);

    @Autowired(required = false)
    private RedisHealthService redisHealthService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("애플리케이션 시작 완료 - Redis 상태 확인");

        if (redisHealthService != null) {
            redisHealthService.logRedisStatus();
        } else {
            logger.info("Redis 서비스가 비활성화되어 있습니다. 로컬 캐시를 사용합니다.");
        }
    }
}