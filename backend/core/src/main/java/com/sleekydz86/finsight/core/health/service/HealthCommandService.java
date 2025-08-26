package com.sleekydz86.finsight.core.health.service;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthCommandUseCase;
import com.sleekydz86.finsight.core.health.domain.port.out.HealthPersistencePort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HealthCommandService implements HealthCommandUseCase {

    private final HealthPersistencePort healthPersistencePort;

    public HealthCommandService(HealthPersistencePort healthPersistencePort) {
        this.healthPersistencePort = healthPersistencePort;
    }

    @Override
    public Health saveHealthCheck(Health health) {
        return healthPersistencePort.save(health);
    }

    @Override
    public void deleteHealthCheck(String id) {
        healthPersistencePort.deleteById(id);
    }

    @Override
    public List<Health> getHealthHistoryByStatus(String status) {
        return healthPersistencePort.findByStatus(status);
    }

    @Override
    public List<Health> getHealthHistoryByDateRange(LocalDateTime start, LocalDateTime end) {
        return healthPersistencePort.findByCheckedAtBetween(start, end);
    }
}