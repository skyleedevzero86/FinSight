package com.sleekydz86.finsight.core.health.domain.vo;

import java.util.Map;

public class HealthStatus {
    private final String status;
    private final String message;
    private final Map<String, String> details;

    public HealthStatus(String status, String message) {
        this(status, message, Map.of());
    }

    public HealthStatus(String status, String message, Map<String, String> details) {
        this.status = status;
        this.message = message;
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return "HealthStatus{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", details=" + details +
                '}';
    }
}