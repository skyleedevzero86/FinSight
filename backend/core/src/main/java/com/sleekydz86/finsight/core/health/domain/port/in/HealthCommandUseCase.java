package com.sleekydz86.finsight.core.health.domain.port.in;

import com.sleekydz86.finsight.core.health.domain.Health;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthCommandUseCase {
    Health saveHealthCheck(Health health);
    void deleteHealthCheck(String id);
    List<Health> getHealthHistoryByStatus(String status);
    List<Health> getHealthHistoryByDateRange(LocalDateTime start, LocalDateTime end);
}