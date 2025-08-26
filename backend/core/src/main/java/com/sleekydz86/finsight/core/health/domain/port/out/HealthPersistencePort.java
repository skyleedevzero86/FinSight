package com.sleekydz86.finsight.core.health.domain.port.out;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthPersistencePort {
    Health save(Health health);
    Optional<Health> findById(String id);
    List<Health> findByStatus(String status);
    List<Health> findByCheckedAtBetween(LocalDateTime start, LocalDateTime end);
    void deleteById(String id);
}