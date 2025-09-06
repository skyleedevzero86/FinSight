package com.sleekydz86.finsight.core.config;

import com.sleekydz86.finsight.core.global.aspect.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AdvancedAopConfig {

    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }

    @Bean
    public PerformanceMonitoringAspect performanceMonitoringAspect() {
        return new PerformanceMonitoringAspect();
    }

    @Bean
    public RetryAspect retryAspect() {
        return new RetryAspect();
    }

    @Bean
    public CachingAspect cachingAspect() {
        return new CachingAspect();
    }
}