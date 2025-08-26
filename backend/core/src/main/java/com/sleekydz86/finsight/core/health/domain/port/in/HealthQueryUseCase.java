package com.sleekydz86.finsight.core.health.domain.port.in;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;

import java.util.Map;

public interface HealthQueryUseCase {
    HealthStatus getOverallHealth();
    Map<String, Object> getDetailedHealth();
    SystemMetrics getSystemMetrics();
    HealthStatus getDatabaseHealth();
    HealthStatus getRedisHealth();
    Map<String, HealthStatus> getExternalApisHealth();
    Health getCompleteHealth();
}