package com.sleekydz86.finsight.core.health.domain.port.out;

import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;

public interface ExternalHealthCheckPort {
    HealthStatus checkDatabaseHealth();
    HealthStatus checkRedisHealth();
    HealthStatus checkExternalApiHealth(String apiName, String endpoint);
}