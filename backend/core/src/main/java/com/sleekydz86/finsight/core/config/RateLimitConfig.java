package com.sleekydz86.finsight.core.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

        private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

        @Value("${security.rate-limit.default-requests-per-minute:100}")
        private int defaultRequestsPerMinute;

        @Value("${security.rate-limit.admin-requests-per-minute:1000}")
        private int adminRequestsPerMinute;

        @Value("${security.rate-limit.strict-requests-per-minute:10}")
        private int strictRequestsPerMinute;

        @Value("${spring.data.redis.host:localhost}")
        private String redisHost;

        @Value("${spring.data.redis.port:6379}")
        private int redisPort;

        @Value("${spring.data.redis.password:}")
        private String redisPassword;

        @Bean
        @Primary
        public Bucket createBucket() {
                logger.info("로컬 메모리 기반 Rate Limit 버킷 생성");
                return Bucket.builder()
                                .addLimit(limit -> limit.capacity(defaultRequestsPerMinute)
                                                .refillGreedy(defaultRequestsPerMinute, Duration.ofMinutes(1)))
                                .build();
        }

        @Bean(name = "strictBucket")
        public Bucket createStrictBucket() {
                return Bucket.builder()
                                .addLimit(limit -> limit.capacity(strictRequestsPerMinute)
                                                .refillGreedy(strictRequestsPerMinute, Duration.ofMinutes(1)))
                                .build();
        }

        @Bean(name = "adminBucket")
        public Bucket createAdminBucket() {
                return Bucket.builder()
                                .addLimit(limit -> limit.capacity(adminRequestsPerMinute)
                                                .refillGreedy(adminRequestsPerMinute, Duration.ofMinutes(1)))
                                .build();
        }

        @Bean(destroyMethod = "shutdown")
        @Profile("core-prod")
        @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
        public RedisClient redisClient() {
                try {
                        String redisUrl = "redis://" + redisHost + ":" + redisPort;
                        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
                                redisUrl = "redis://:" + redisPassword + "@" + redisHost + ":" + redisPort;
                        }
                        logger.info("Redis 클라이언트 생성: {}", redisUrl.replaceAll("redis://:.*@", "redis://:***@"));
                        return RedisClient.create(redisUrl);
                } catch (Exception e) {
                        logger.error("Redis 클라이언트 생성 실패: {}", e.getMessage());
                        throw new RuntimeException("Redis 클라이언트 생성 실패", e);
                }
        }

        @Bean(destroyMethod = "close")
        @Profile("core-prod")
        @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
        public StatefulRedisConnection<byte[], byte[]> redisConnection(RedisClient redisClient) {
                try {
                        logger.info("Redis 연결 생성");
                        return redisClient.connect(new ByteArrayCodec());
                } catch (Exception e) {
                        logger.error("Redis 연결 생성 실패: {}", e.getMessage());
                        throw new RuntimeException("Redis 연결 생성 실패", e);
                }
        }

        @Bean
        @Profile("core-prod")
        @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
        public ProxyManager<byte[]> proxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
                try {
                        logger.info("Redis Proxy Manager 생성");
                        return LettuceBasedProxyManager.builderFor(connection).build();
                } catch (Exception e) {
                        logger.error("Redis Proxy Manager 생성 실패: {}", e.getMessage());
                        throw new RuntimeException("Redis Proxy Manager 생성 실패", e);
                }
        }

        @Bean(name = "distributedBucket")
        @Profile("core-prod")
        @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
        public Bucket createDistributedBucket(ProxyManager<byte[]> proxyManager) {
                try {
                        BucketConfiguration configuration = BucketConfiguration.builder()
                                        .addLimit(limit -> limit.capacity(defaultRequestsPerMinute)
                                                        .refillGreedy(defaultRequestsPerMinute, Duration.ofMinutes(1)))
                                        .build();
                        logger.info("분산 Rate Limit 버킷 생성");
                        return proxyManager.builder().build("default-bucket".getBytes(), configuration);
                } catch (Exception e) {
                        logger.error("분산 Rate Limit 버킷 생성 실패: {}", e.getMessage());
                        throw new RuntimeException("분산 Rate Limit 버킷 생성 실패", e);
                }
        }

        @Bean(name = "distributedStrictBucket")
        @Profile("core-prod")
        @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
        public Bucket createDistributedStrictBucket(ProxyManager<byte[]> proxyManager) {
                try {
                        BucketConfiguration configuration = BucketConfiguration.builder()
                                        .addLimit(limit -> limit.capacity(strictRequestsPerMinute)
                                                        .refillGreedy(strictRequestsPerMinute, Duration.ofMinutes(1)))
                                        .build();
                        return proxyManager.builder().build("strict-bucket".getBytes(), configuration);
                } catch (Exception e) {
                        logger.error("분산 Strict Rate Limit 버킷 생성 실패: {}", e.getMessage());
                        throw new RuntimeException("분산 Strict Rate Limit 버킷 생성 실패", e);
                }
        }

        @Bean(name = "distributedAdminBucket")
        @Profile("core-prod")
        @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
        public Bucket createDistributedAdminBucket(ProxyManager<byte[]> proxyManager) {
                try {
                        BucketConfiguration configuration = BucketConfiguration.builder()
                                        .addLimit(limit -> limit.capacity(adminRequestsPerMinute)
                                                        .refillGreedy(adminRequestsPerMinute, Duration.ofMinutes(1)))
                                        .build();
                        return proxyManager.builder().build("admin-bucket".getBytes(), configuration);
                } catch (Exception e) {
                        logger.error("분산 Admin Rate Limit 버킷 생성 실패: {}", e.getMessage());
                        throw new RuntimeException("분산 Admin Rate Limit 버킷 생성 실패", e);
                }
        }
}