package com.sleekydz86.finsight.core.global.config;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class NewsAiCircuitBreakerConfig {

    @Bean
    public CircuitBreakerConfigCustomizer newsAiCircuitBreakerCustomizer() {
        return CircuitBreakerConfigCustomizer.of("newsAiCircuitBreaker", builder -> builder
                .failureRateThreshold(50)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(2));
    }
}
