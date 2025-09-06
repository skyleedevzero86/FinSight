package com.sleekydz86.finsight.core.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdvancedMetricsConfig {

    @Bean
    public Counter requestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("http.requests.total")
                .description("Total number of HTTP requests")
                .register(meterRegistry);
    }

    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("http.request.duration")
                .description("HTTP request duration")
                .register(meterRegistry);
    }

    @Bean
    public Counter errorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("http.errors.total")
                .description("Total number of HTTP errors")
                .register(meterRegistry);
    }
}