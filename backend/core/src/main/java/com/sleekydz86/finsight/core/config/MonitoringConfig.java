package com.sleekydz86.finsight.core.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitoringConfig {

    @Bean
    public Timer requestTimer(MeterRegistry meterRegistry) {
        return Timer.builder("http.requests")
                .description("HTTP request duration")
                .register(meterRegistry);
    }
}