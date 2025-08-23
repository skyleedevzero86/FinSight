package com.sleekydz86.finsight.core.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class LoggingConfig {

    @PostConstruct
    public void init() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();

        // 로그 설정 파일 로드
        try {
            configurator.doConfigure(getClass().getResourceAsStream("/logback-spring.xml"));
        } catch (Exception e) {
            // 기본 설정 사용
        }
    }
}