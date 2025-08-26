package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.out.HealthPersistencePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class HealthRepositoryImpl implements HealthPersistencePort {

    private final HealthJpaRepository healthJpaRepository;
    private final HealthJpaMapper healthJpaMapper;
    private final ObjectMapper objectMapper;

    public HealthRepositoryImpl(HealthJpaRepository healthJpaRepository,
                                HealthJpaMapper healthJpaMapper,
                                ObjectMapper objectMapper) {
        this.healthJpaRepository = healthJpaRepository;
        this.healthJpaMapper = healthJpaMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Health save(Health health) {
        HealthJpaEntity entity = healthJpaMapper.toJpaEntity(health);
        HealthJpaEntity savedEntity = healthJpaRepository.save(entity);
        return healthJpaMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Health> findById(String id) {
        return healthJpaRepository.findById(id)
                .map(healthJpaMapper::toDomain);
    }

    @Override
    public List<Health> findByStatus(String status) {
        return healthJpaRepository.findByStatus(status)
                .stream()
                .map(healthJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Health> findByCheckedAtBetween(LocalDateTime start, LocalDateTime end) {
        return healthJpaRepository.findByCheckedAtBetween(start, end)
                .stream()
                .map(healthJpaMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        healthJpaRepository.deleteById(id);
    }
}