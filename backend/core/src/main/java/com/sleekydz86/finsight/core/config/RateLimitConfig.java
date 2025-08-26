package com.sleekydz86.finsight.core.config;

import com.bucket4j.Bandwidth;
import com.bucket4j.Bucket;
import com.bucket4j.BucketConfiguration;
import com.bucket4j.Refill;
import com.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.LettuceProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Value("${security.rate-limit.default-requests-per-minute:100}")
    private int defaultRequestsPerMinute;

    @Value("${security.rate-limit.admin-requests-per-minute:1000}")
    private int adminRequestsPerMinute;

    @Value("${security.rate-limit.strict-requests-per-minute:10}")
    private int strictRequestsPerMinute;

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

    @Bean
    @Profile("core-prod")
    public ProxyManager<String> proxyManager(RedisConnectionFactory connectionFactory) {
        return LettuceProxyManager.builderFor(connectionFactory).build();
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