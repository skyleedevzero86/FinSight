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
                "jwt.secret"
        };

        for (String prop : requiredProps) {
            String value = environment.getProperty(prop);
            if (value == null || value.trim().isEmpty()) {
                logger.error("필수 속성이 누락되었습니다: {}", prop);
                throw new IllegalStateException("필수 속성이 누락되었습니다: " + prop);
            }
        }
    }

    private void validateSecurityProperties() {
        String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret != null && jwtSecret.length() < 256) {
            logger.warn("JWT 시크릿 길이가 권장값보다 짧습니다. 권장 최소 길이: 256자, 현재 길이: {}", jwtSecret.length());
        }

        String minLength = environment.getProperty("security.password.min-length");
        if (minLength != null && Integer.parseInt(minLength) < 8) {
            logger.warn("비밀번호 최소 길이가 권장값보다 낮습니다. 권장 최소 길이: 8, 현재 값: {}", minLength);
        }
    }

    private void validateDatabaseProperties() {
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (dbUrl != null && !dbUrl.contains("useSSL=true")) {
            logger.warn("데이터베이스 연결에 SSL이 활성화되어 있지 않습니다. 로컬 개발 환경 외에서는 SSL 사용을 권장합니다.");
        }

        String dbPassword = environment.getProperty("spring.datasource.password");
        if (dbPassword != null && dbPassword.length() < 8) {
            logger.warn("데이터베이스 비밀번호 길이가 권장값보다 짧습니다. 권장 최소 길이: 8자");
        }
    }

    private void validateApiProperties() {
        String[] apiKeys = {
                "news.marketaux.api.api-key",
                "ai.openai.api.api-key"
        };

        for (String apiKey : apiKeys) {
            String value = environment.getProperty(apiKey);
            if (value == null || value.isBlank()) {
                logger.warn("선택 API 키가 비어 있습니다. 해당 기능은 비활성 상태로 기동합니다: {}", apiKey);
            } else if (value.startsWith("sk-") && value.length() < 50) {
                logger.warn("API 키 길이가 예상보다 짧아 보입니다: {}", apiKey);
            }
        }
    }

    public void logEnvironmentSummary() {
        logger.info("=== 환경 설정 요약 ===");
        logger.info("활성 프로필: {}", Arrays.toString(environment.getActiveProfiles()));
        logger.info("데이터베이스 URL: {}", maskSensitiveValue(environment.getProperty("spring.datasource.url")));
        logger.info("Redis 호스트: {}", environment.getProperty("spring.data.redis.host"));
        logger.info("JWT 만료 시간: {}ms", environment.getProperty("jwt.access-token.expiration", "3600000"));
        logger.info("요청 제한: 분당 {}회", environment.getProperty("security.rate-limit.requests-per-minute"));
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
