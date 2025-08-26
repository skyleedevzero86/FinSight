package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthCommandUseCase;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthQueryUseCase;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthQueryUseCase healthQueryUseCase;
    private final HealthCommandUseCase healthCommandUseCase;

    public HealthController(HealthQueryUseCase healthQueryUseCase,
                            HealthCommandUseCase healthCommandUseCase) {
        this.healthQueryUseCase = healthQueryUseCase;
        this.healthCommandUseCase = healthCommandUseCase;
    }

    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        HealthStatus status = healthQueryUseCase.getOverallHealth();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> detailedStatus = healthQueryUseCase.getDetailedHealth();
        return ResponseEntity.ok(detailedStatus);
    }

    @GetMapping("/metrics")
    public ResponseEntity<SystemMetrics> metrics() {
        SystemMetrics metrics = healthQueryUseCase.getSystemMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/database")
    public ResponseEntity<HealthStatus> databaseHealth() {
        HealthStatus status = healthQueryUseCase.getDatabaseHealth();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/redis")
    public ResponseEntity<HealthStatus> redisHealth() {
        HealthStatus status = healthQueryUseCase.getRedisHealth();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/external-apis")
    public ResponseEntity<Map<String, HealthStatus>> externalApisHealth() {
        Map<String, HealthStatus> statuses = healthQueryUseCase.getExternalApisHealth();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/complete")
    public ResponseEntity<Health> completeHealth() {
        Health completeHealth = healthQueryUseCase.getCompleteHealth();
        return ResponseEntity.ok(completeHealth);
    }

    @PostMapping("/save")
    public ResponseEntity<Health> saveHealthCheck(@RequestBody Health health) {
        Health savedHealth = healthCommandUseCase.saveHealthCheck(health);
        return ResponseEntity.ok(savedHealth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHealthCheck(@PathVariable String id) {
        healthCommandUseCase.deleteHealthCheck(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history/status/{status}")
    public ResponseEntity<List<Health>> getHealthHistoryByStatus(@PathVariable String status) {
        List<Health> history = healthCommandUseCase.getHealthHistoryByStatus(status);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/history/date-range")
    public ResponseEntity<List<Health>> getHealthHistoryByDateRange(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        List<Health> history = healthCommandUseCase.getHealthHistoryByDateRange(start, end);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/exception")
    public String exception() {
        throw new IllegalArgumentException("예외 발생!");
    }
}