package com.sleekydz86.finsight.core.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket createBucket() {

        Refill refill = Refill.intervally(50, Duration.ofMinutes(1));

        Bandwidth bandwidth = Bandwidth.classic(50, refill);

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    @Bean(name = "strictBucket")
    public Bucket createStrictBucket() {
        Refill refill = Refill.intervally(5, Duration.ofMinutes(1));
        Bandwidth bandwidth = Bandwidth.classic(5, refill);

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    @Bean(name = "adminBucket")
    public Bucket createAdminBucket() {
        Refill refill = Refill.intervally(200, Duration.ofMinutes(1));
        Bandwidth bandwidth = Bandwidth.classic(200, refill);

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}