package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HealthJpaRepository extends JpaRepository<HealthJpaEntity, String> {
    List<HealthJpaEntity> findByStatus(String status);
    List<HealthJpaEntity> findByCheckedAtBetween(LocalDateTime start, LocalDateTime end);
}