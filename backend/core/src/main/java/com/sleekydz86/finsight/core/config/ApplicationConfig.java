package com.sleekydz86.finsight.core.config;

import com.sleekydz86.finsight.core.global.aspect.*;
import com.sleekydz86.finsight.core.global.cache.AdvancedCacheManager;
import com.sleekydz86.finsight.core.global.logging.StructuredLogger;
import com.sleekydz86.finsight.core.global.metrics.MetricsCollector;
import com.sleekydz86.finsight.core.global.security.SecurityAuditService;
import com.sleekydz86.finsight.core.global.validation.ValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAspectJAutoProxy
@Import({
        AdvancedAopConfig.class,
        AdvancedCacheConfig.class,
        AdvancedDatabaseConfig.class,
        AdvancedSecurityConfig.class,
        AdvancedAsyncConfig.class,
        AdvancedMonitoringConfig.class,
        AdvancedMetricsConfig.class
})
public class ApplicationConfig {

    @Bean
    public StructuredLogger structuredLogger() {
        return new StructuredLogger();
    }

    @Bean
    public MetricsCollector metricsCollector() {
        return new MetricsCollector();
    }

    @Bean
    public SecurityAuditService securityAuditService() {
        return new SecurityAuditService();
    }

    @Bean
    public ValidationService validationService() {
        return new ValidationService();
    }

    @Bean
    public AdvancedCacheManager advancedCacheManager() {
        return new AdvancedCacheManager(null);
    }
}