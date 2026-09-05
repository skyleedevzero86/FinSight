package com.sleekydz86.finsight.core.health.adapter.out;

import com.sleekydz86.finsight.core.global.config.RedisHealthService;
import com.sleekydz86.finsight.core.health.domain.port.out.ExternalHealthCheckPort;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.util.Locale;
import java.util.Map;

@Component
public class ExternalHealthCheckAdapter implements ExternalHealthCheckPort {

    private final DataSource dataSource;
    private final ObjectProvider<RedisHealthService> redisHealthServiceProvider;
    private final Map<String, String> externalApiBases;

    public ExternalHealthCheckAdapter(
            DataSource dataSource,
            ObjectProvider<RedisHealthService> redisHealthServiceProvider,
            @Value("${finsight.health.external.openai-base-url:}") String openaiBaseUrl,
            @Value("${finsight.health.external.marketaux-base-url:}") String marketauxBaseUrl) {
        this.dataSource = dataSource;
        this.redisHealthServiceProvider = redisHealthServiceProvider;
        this.externalApiBases = Map.of(
                "openai", openaiBaseUrl == null ? "" : openaiBaseUrl.trim(),
                "marketaux", marketauxBaseUrl == null ? "" : marketauxBaseUrl.trim());
    }

    @Override
    public HealthStatus checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                return new HealthStatus("UP", "DB가 정상입니다");
            }
            return new HealthStatus("DOWN", "DB 연결 검증에 실패했습니다");
        } catch (Exception e) {
            return new HealthStatus("DOWN", "DB 헬스체크 실패: " + e.getMessage());
        }
    }

    @Override
    public HealthStatus checkRedisHealth() {
        RedisHealthService redisHealthService = redisHealthServiceProvider.getIfAvailable();
        if (redisHealthService == null) {
            return new HealthStatus("UNKNOWN", "Redis 템플릿이 구성되어 있지 않습니다");
        }
        try {
            if (redisHealthService.isRedisAvailable()) {
                return new HealthStatus("UP", "Redis가 정상입니다");
            }
            return new HealthStatus("DOWN", "Redis 연결 확인에 실패했습니다");
        } catch (Exception e) {
            return new HealthStatus("DOWN", "Redis 헬스체크 실패: " + e.getMessage());
        }
    }

    @Override
    public HealthStatus checkExternalApiHealth(String apiName, String endpoint) {
        String key = apiName == null ? "" : apiName.trim().toLowerCase(Locale.ROOT);
        String base = externalApiBases.getOrDefault(key, "");
        if (base.isBlank()) {
            return new HealthStatus("UNKNOWN", apiName + " 기본 URL이 설정되지 않았습니다");
        }
        try {
            String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
            String path = endpoint == null || endpoint.isBlank()
                    ? ""
                    : (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
            URL url = URI.create(normalizedBase + path).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                return new HealthStatus("UP", apiName + " API가 정상입니다");
            }
            return new HealthStatus("DOWN", apiName + " API 응답 상태: " + responseCode);
        } catch (Exception e) {
            return new HealthStatus("DOWN", apiName + " API 헬스체크 실패: " + e.getMessage());
        }
    }
}
