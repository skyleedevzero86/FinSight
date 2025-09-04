package com.sleekydz86.finsight.core.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${security.rate-limit.default-requests-per-minute:100}")
    private int defaultRequestsPerMinute;

    @Value("${security.rate-limit.admin-requests-per-minute:1000}")
    private int adminRequestsPerMinute;

    @Value("${security.rate-limit.strict-requests-per-minute:10}")
    private int strictRequestsPerMinute;

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Bean
    public Bucket createBucket() {
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
    public RedisClient redisClient() {
        return RedisClient.create("redis://" + redisHost + ":" + redisPort);
    }

    @Bean(destroyMethod = "close")
    @Profile("core-prod")
    public StatefulRedisConnection<byte[], byte[]> redisConnection(RedisClient redisClient) {
        return redisClient.connect(new ByteArrayCodec());
    }

    @Bean
    @Profile("core-prod")
    public ProxyManager<byte[]> proxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    @Bean
    @Profile("core-prod")
    public Bucket createDistributedBucket(ProxyManager<byte[]> proxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(defaultRequestsPerMinute)
                        .refillGreedy(defaultRequestsPerMinute, Duration.ofMinutes(1)))
                .build();
        return proxyManager.builder().build("default-bucket".getBytes(), configuration);
    }

    @Bean(name = "distributedStrictBucket")
    @Profile("core-prod")
    public Bucket createDistributedStrictBucket(ProxyManager<byte[]> proxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(strictRequestsPerMinute)
                        .refillGreedy(strictRequestsPerMinute, Duration.ofMinutes(1)))
                .build();
        return proxyManager.builder().build("strict-bucket".getBytes(), configuration);
    }

    @Bean(name = "distributedAdminBucket")
    @Profile("core-prod")
    public Bucket createDistributedAdminBucket(ProxyManager<byte[]> proxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(adminRequestsPerMinute)
                        .refillGreedy(adminRequestsPerMinute, Duration.ofMinutes(1)))
                .build();
        return proxyManager.builder().build("admin-bucket".getBytes(), configuration);
    }
}