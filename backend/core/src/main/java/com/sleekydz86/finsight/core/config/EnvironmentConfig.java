package com.sleekydz86.finsight.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Configuration
public class EnvironmentConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);

    private final Environment environment;

    private static final List<String> SENSITIVE_KEYS = Arrays.asList(
            "jwt.secret", "jwt.refresh-secret", "encrypt.key", "encrypt.password",
            "api.key", "api.secret", "database.password", "redis.password",
            "mail.password", "sms.api.key", "payment.secret"
    );

    public EnvironmentConfig(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateEnvironmentConfiguration() {
        logger.info("=== 환경 설정 검증 시작 ===");

        validateRequiredProperties();
        validateSecurityProperties();
        validateDatabaseProperties();
        validateApiProperties();

        logger.info("=== 환경 설정 검증 완료 ===");
    }

    private void validateRequiredProperties() {
        String[] requiredProps = {
                "spring.datasource.url",
                "spring.datasource.username",
                "jwt.secret",
                "jwt.expiration-period"
        };

        for (String prop : requiredProps) {
            String value = environment.getProperty(prop);
            if (value == null || value.trim().isEmpty()) {
                logger.error("필수 환경 변수가 설정되지 않았습니다: {}", prop);
                throw new IllegalStateException("필수 환경 변수 누락: " + prop);
            }
        }
    }

    private void validateSecurityProperties() {
        String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret != null && jwtSecret.length() < 256) {
            logger.warn("JWT 시크릿 키가 너무 짧습니다. 최소 256자 이상 권장. 현재 길이: {}", jwtSecret.length());
        }

        String minLength = environment.getProperty("security.password.min-length");
        if (minLength != null && Integer.parseInt(minLength) < 12) {
            logger.warn("비밀번호 최소 길이가 너무 짧습니다. 최소 12자 이상 권장. 현재 설정: {}", minLength);
        }
    }

    private void validateDatabaseProperties() {
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (dbUrl != null && !dbUrl.contains("useSSL=true")) {
            logger.warn("데이터베이스 연결에 SSL이 활성화되지 않았습니다. 운영 환경에서는 SSL 사용을 권장합니다.");
        }

        String dbPassword = environment.getProperty("spring.datasource.password");
        if (dbPassword != null && dbPassword.length() < 8) {
            logger.warn("데이터베이스 비밀번호가 너무 짧습니다. 최소 8자 이상 권장.");
        }
    }

    private void validateApiProperties() {
        String[] apiKeys = {
                "news.marketaux.api.api-key",
                "ai.openai.api.api-key"
        };

        for (String apiKey : apiKeys) {
            String value = environment.getProperty(apiKey);
            if (value != null && value.startsWith("sk-") && value.length() < 50) {
                logger.warn("API 키가 너무 짧거나 형식이 올바르지 않습니다: {}", apiKey);
            }
        }
    }

    public void logEnvironmentSummary() {
        logger.info("=== 환경 설정 요약 ===");
        logger.info("Active Profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        logger.info("Database URL: {}", maskSensitiveValue(environment.getProperty("spring.datasource.url")));
        logger.info("Redis Host: {}", environment.getProperty("spring.data.redis.host"));
        logger.info("JWT Expiration: {}ms", environment.getProperty("jwt.expiration-period"));
        logger.info("Rate Limit: {} requests/min", environment.getProperty("security.rate-limit.requests-per-minute"));
    }

    private String maskSensitiveValue(String value) {
        if (value == null) return "null";

        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (value.contains(sensitiveKey)) {
                return value.replaceAll("([^=]*=)([^,]*)(,|$)", "$1***$3");
            }
        }

        return value;
    }
}