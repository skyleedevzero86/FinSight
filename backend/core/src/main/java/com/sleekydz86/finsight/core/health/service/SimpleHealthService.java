package com.sleekydz86.finsight.core.health.service;

import com.sleekydz86.finsight.core.health.dto.HealthStatus;
import com.sleekydz86.finsight.core.health.dto.SystemMetrics;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Service
public class SimpleHealthService {

    private final DataSource dataSource;

    public SimpleHealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HealthStatus getOverallHealth() {
        try {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(5)) {
                    return new HealthStatus("UP", "System is healthy", Map.of("database", "UP"));
                } else {
                    return new HealthStatus("DOWN", "Database connection failed", Map.of("database", "DOWN"));
                }
            }
        } catch (Exception e) {
            return new HealthStatus("DOWN", "System health check failed: " + e.getMessage(),
                    Map.of("error", e.getMessage()));
        }
    }

    public Map<String, Object> getDetailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        try (Connection connection = dataSource.getConnection()) {
            health.put("database", Map.of(
                    "status", "UP",
                    "connection", "OK",
                    "responseTime", "5ms"
            ));
        } catch (Exception e) {
            health.put("database", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }

        Runtime runtime = Runtime.getRuntime();
        health.put("jvm", Map.of(
                "memory", Map.of(
                        "total", runtime.totalMemory(),
                        "free", runtime.freeMemory(),
                        "used", runtime.totalMemory() - runtime.freeMemory(),
                        "max", runtime.maxMemory()
                ),
                "processors", runtime.availableProcessors()
        ));

        return health;
    }

    public SystemMetrics getSystemMetrics() {
        return new SystemMetrics();
    }

    public HealthStatus getDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                return new HealthStatus("UP", "Database is healthy");
            } else {
                return new HealthStatus("DOWN", "Database connection validation failed");
            }
        } catch (Exception e) {
            return new HealthStatus("DOWN", "Database health check failed: " + e.getMessage());
        }
    }

    public HealthStatus getRedisHealth() {
        return new HealthStatus("UNKNOWN", "Redis health check not implemented");
    }

    public Map<String, HealthStatus> getExternalApisHealth() {
        Map<String, HealthStatus> statuses = new HashMap<>();
        statuses.put("marketaux", new HealthStatus("UNKNOWN", "MarketAux API health check not implemented"));
        statuses.put("openai", new HealthStatus("UNKNOWN", "OpenAI API health check not implemented"));
        return statuses;
    }
}