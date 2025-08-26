package com.sleekydz86.finsight.core.health.domain;

import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;

import java.time.LocalDateTime;
import java.util.Map;

public class Health {
    private final String id;
    private final HealthStatus status;
    private final SystemMetrics metrics;
    private final Map<String, HealthStatus> componentStatuses;
    private final LocalDateTime checkedAt;

    public Health(String id, HealthStatus status, SystemMetrics metrics,
                  Map<String, HealthStatus> componentStatuses) {
        this.id = id;
        this.status = status;
        this.metrics = metrics;
        this.componentStatuses = componentStatuses;
        this.checkedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public SystemMetrics getMetrics() {
        return metrics;
    }

    public Map<String, HealthStatus> getComponentStatuses() {
        return componentStatuses;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    @Override
    public String toString() {
        return "Health{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", metrics=" + metrics +
                ", componentStatuses=" + componentStatuses +
                ", checkedAt=" + checkedAt +
                '}';
    }
}