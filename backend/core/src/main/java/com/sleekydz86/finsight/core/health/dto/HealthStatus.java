package com.sleekydz86.finsight.core.health.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class HealthStatus {
    private String status;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, Object> details;
    private long responseTime;

    public HealthStatus() {}

    public HealthStatus(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public HealthStatus(String status, String message, Map<String, Object> details) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }

    public HealthStatus(String status, String message, Map<String, Object> details, long responseTime) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.responseTime = responseTime;
        this.timestamp = LocalDateTime.now();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }

    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
}