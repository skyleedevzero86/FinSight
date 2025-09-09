package com.sleekydz86.finsight.core.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdvancedMonitoringConfig {

    @Bean
    public Counter monitoringRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("monitoring.http.requests.total")
                .description("Total number of HTTP requests for monitoring")
                .register(meterRegistry);
    }

    @Bean
    public Counter monitoringErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("monitoring.http.errors.total")
                .description("Total number of HTTP errors for monitoring")
                .register(meterRegistry);
    }

    @Bean
    public Timer monitoringRequestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("monitoring.http.request.duration")
                .description("HTTP request duration for monitoring")
                .register(meterRegistry);
    }

    @Bean
    public Counter businessLogicCounter(MeterRegistry meterRegistry) {
        return Counter.builder("business.logic.executions")
                .description("Total number of business logic executions")
                .register(meterRegistry);
    }
}