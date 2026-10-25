package com.sleekydz86.finsight.core.global.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class NewsAiCircuitBreakerConfig {

    @Bean
    public CircuitBreaker newsAiCircuitBreaker(
            CircuitBreakerRegistry registry,
            @Value("${finsight.news-ai.circuit-breaker.wait-duration-in-open-state:60s}") Duration openWait,
            @Value("${finsight.news-ai.circuit-breaker.slow-call-duration-threshold:20s}") Duration slowCall) {
        return registry.circuitBreaker("newsAiCircuitBreaker", CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(slowCall)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(openWait)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
    }
}
