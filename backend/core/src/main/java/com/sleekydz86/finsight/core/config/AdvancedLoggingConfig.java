package com.sleekydz86.finsight.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@Configuration
public class AdvancedLoggingConfig {

    @Bean
    @Profile("prod")
    public LoggerContext loggerContext() {
        LoggerContext context = new LoggerContext();

        RollingFileAppender appender = new RollingFileAppender();
        appender.setContext(context);
        appender.setName("FILE");
        appender.setFile("logs/finsight.log");

        TimeBasedRollingPolicy rollingPolicy = new TimeBasedRollingPolicy();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(appender);
        rollingPolicy.setFileNamePattern("logs/finsight.%d{yyyy-MM-dd}.%i.log");
        rollingPolicy.setMaxHistory(30);
        rollingPolicy.setTotalSizeCap("1GB");
        rollingPolicy.start();

        appender.setRollingPolicy(rollingPolicy);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        appender.setEncoder(encoder);
        appender.start();

        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
        rootLogger.addAppender(appender);

        return context;
    }
}
