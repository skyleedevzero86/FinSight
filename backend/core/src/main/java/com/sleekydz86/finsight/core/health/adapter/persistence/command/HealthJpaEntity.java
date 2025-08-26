package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_checks")
public class HealthJpaEntity {

    @Id
    private String id;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "jvm_metrics", columnDefinition = "TEXT")
    private String jvmMetricsJson;

    @Column(name = "system_metrics", columnDefinition = "TEXT")
    private String systemMetricsJson;

    @Column(name = "component_statuses", columnDefinition = "TEXT")
    private String componentStatusesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public String getJvmMetricsJson() { return jvmMetricsJson; }
    public void setJvmMetricsJson(String jvmMetricsJson) { this.jvmMetricsJson = jvmMetricsJson; }

    public String getSystemMetricsJson() { return systemMetricsJson; }
    public void setSystemMetricsJson(String systemMetricsJson) { this.systemMetricsJson = systemMetricsJson; }

    public String getComponentStatusesJson() { return componentStatusesJson; }
    public void setComponentStatusesJson(String componentStatusesJson) { this.componentStatusesJson = componentStatusesJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}