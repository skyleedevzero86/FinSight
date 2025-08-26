package com.sleekydz86.finsight.core.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.RedisCodec;
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

    @Value("${spring.redis.port:9379}")
    private int redisPort;

    @Bean
    public Bucket createBucket() {
        Bandwidth limit = Bandwidth.classic(defaultRequestsPerMinute,
                Refill.greedy(defaultRequestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Bean(name = "strictBucket")
    public Bucket createStrictBucket() {
        Bandwidth limit = Bandwidth.classic(strictRequestsPerMinute,
                Refill.greedy(strictRequestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Bean(name = "adminBucket")
    public Bucket createAdminBucket() {
        Bandwidth limit = Bandwidth.classic(adminRequestsPerMinute,
                Refill.greedy(adminRequestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    @Bean(destroyMethod = "shutdown")
    @Profile("core-prod")
    public RedisClient redisClient() {
        String uri = "redis://" + redisHost + ":" + redisPort;
        return RedisClient.create(uri);
    }

    @Bean(destroyMethod = "close")
    @Profile("core-prod")
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);
        return redisClient.connect(codec);
    }

    @Bean
    @Profile("core-prod")
    public ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    @Bean
    @Profile("core-prod")
    public Bucket createDistributedBucket(ProxyManager<String> proxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(defaultRequestsPerMinute,
                        Refill.greedy(defaultRequestsPerMinute, Duration.ofMinutes(1))))
                .build();

        return proxyManager.builder()
                .build("default-bucket", () -> configuration);
    }

    @Bean(name = "distributedStrictBucket")
    @Profile("core-prod")
    public Bucket createDistributedStrictBucket(ProxyManager<String> proxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(strictRequestsPerMinute,
                        Refill.greedy(strictRequestsPerMinute, Duration.ofMinutes(1))))
                .build();

        return proxyManager.builder()
                .build("strict-bucket", () -> configuration);
    }

    @Bean(name = "distributedAdminBucket")
    @Profile("core-prod")
    public Bucket createDistributedAdminBucket(ProxyManager<String> proxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(adminRequestsPerMinute,
                        Refill.greedy(adminRequestsPerMinute, Duration.ofMinutes(1))))
                .build();

        return proxyManager.builder()
                .build("admin-bucket", () -> configuration);
    }
}
