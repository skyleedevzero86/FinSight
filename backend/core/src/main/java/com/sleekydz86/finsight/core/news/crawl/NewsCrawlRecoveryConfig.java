package com.sleekydz86.finsight.core.news.crawl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class NewsCrawlRecoveryConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock newsCrawlClock() {
        return Clock.systemDefaultZone();
    }
}
