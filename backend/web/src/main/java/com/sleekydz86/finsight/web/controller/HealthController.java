package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.health.dto.HealthStatus;
import com.sleekydz86.finsight.core.health.dto.SystemMetrics;
import com.sleekydz86.finsight.core.health.service.SimpleHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final SimpleHealthService healthService;

    public HealthController(SimpleHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        HealthStatus status = healthService.getOverallHealth();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> detailedStatus = healthService.getDetailedHealth();
        return ResponseEntity.ok(detailedStatus);
    }

    @GetMapping("/metrics")
    public ResponseEntity<SystemMetrics> metrics() {
        SystemMetrics metrics = healthService.getSystemMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/database")
    public ResponseEntity<HealthStatus> databaseHealth() {
        HealthStatus status = healthService.getDatabaseHealth();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/redis")
    public ResponseEntity<HealthStatus> redisHealth() {
        HealthStatus status = healthService.getRedisHealth();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/external-apis")
    public ResponseEntity<Map<String, HealthStatus>> externalApisHealth() {
        Map<String, HealthStatus> statuses = healthService.getExternalApisHealth();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/exception")
    public String exception() {
        throw new IllegalArgumentException("예외 발생!");
    }
}