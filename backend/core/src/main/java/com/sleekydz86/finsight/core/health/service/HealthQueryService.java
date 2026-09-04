package com.sleekydz86.finsight.core.health.service;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthQueryUseCase;
import com.sleekydz86.finsight.core.health.domain.port.out.ExternalHealthCheckPort;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class HealthQueryService implements HealthQueryUseCase {

    private final ExternalHealthCheckPort externalHealthCheckPort;

    public HealthQueryService(ExternalHealthCheckPort externalHealthCheckPort) {
        this.externalHealthCheckPort = externalHealthCheckPort;
    }

    @Override
    public HealthStatus getOverallHealth() {
        try {
            HealthStatus dbHealth = getDatabaseHealth();
            HealthStatus redisHealth = getRedisHealth();

            if ("UP".equals(dbHealth.getStatus()) && "UP".equals(redisHealth.getStatus())) {
                return new HealthStatus("UP", "시스템이 정상입니다",
                        Map.of("database", "UP", "redis", "UP"));
            } else {
                return new HealthStatus("DOWN", "일부 구성 요소에 장애가 있습니다",
                        Map.of("database", dbHealth.getStatus(), "redis", redisHealth.getStatus()));
            }
        } catch (Exception e) {
            return new HealthStatus("DOWN", "시스템 헬스체크 실패: " + e.getMessage(),
                    Map.of("error", e.getMessage()));
        }
    }

    @Override
    public Map<String, Object> getDetailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        try {
            health.put("database", Map.of(
                    "status", getDatabaseHealth().getStatus(),
                    "message", getDatabaseHealth().getMessage()));
        } catch (Exception e) {
            health.put("database", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()));
        }

        try {
            health.put("redis", Map.of(
                    "status", getRedisHealth().getStatus(),
                    "message", getRedisHealth().getMessage()));
        } catch (Exception e) {
            health.put("redis", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()));
        }

        health.put("jvm", getSystemMetrics().getJvmMetrics());
        health.put("system", getSystemMetrics().getSystemMetrics());

        return health;
    }

    @Override
    public SystemMetrics getSystemMetrics() {
        return new SystemMetrics();
    }

    @Override
    public HealthStatus getDatabaseHealth() {
        return externalHealthCheckPort.checkDatabaseHealth();
    }

    @Override
    public HealthStatus getRedisHealth() {
        return externalHealthCheckPort.checkRedisHealth();
    }

    @Override
    public Map<String, HealthStatus> getExternalApisHealth() {
        Map<String, HealthStatus> statuses = new HashMap<>();
        statuses.put("marketaux", externalHealthCheckPort.checkExternalApiHealth("marketaux", "/api/health"));
        statuses.put("openai", externalHealthCheckPort.checkExternalApiHealth("openai", "/api/health"));
        return statuses;
    }

    @Override
    public Health getCompleteHealth() {
        String id = UUID.randomUUID().toString();
        HealthStatus overallStatus = getOverallHealth();
        SystemMetrics metrics = getSystemMetrics();
        Map<String, HealthStatus> componentStatuses = new HashMap<>();

        componentStatuses.put("database", getDatabaseHealth());
        componentStatuses.put("redis", getRedisHealth());
        componentStatuses.putAll(getExternalApisHealth());

        return new Health(id, overallStatus, metrics, componentStatuses);
    }
}