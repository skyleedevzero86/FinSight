package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HealthJpaMapper {

    private final ObjectMapper objectMapper;

    public HealthJpaMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HealthJpaEntity toJpaEntity(Health health) {
        HealthJpaEntity entity = new HealthJpaEntity();
        entity.setId(health.getId());
        entity.setStatus(health.getStatus().getStatus());
        entity.setMessage(health.getStatus().getMessage());
        entity.setCheckedAt(health.getCheckedAt());

        try {
            entity.setJvmMetricsJson(objectMapper.writeValueAsString(health.getMetrics().getJvmMetrics()));
            entity.setSystemMetricsJson(objectMapper.writeValueAsString(health.getMetrics().getSystemMetrics()));
            entity.setComponentStatusesJson(objectMapper.writeValueAsString(health.getComponentStatuses()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize health data", e);
        }

        return entity;
    }

    public Health toDomain(HealthJpaEntity entity) {
        try {
            Map<String, Object> jvmMetrics = objectMapper.readValue(entity.getJvmMetricsJson(), Map.class);
            Map<String, Object> systemMetrics = objectMapper.readValue(entity.getSystemMetricsJson(), Map.class);
            Map<String, String> componentStatuses = objectMapper.readValue(entity.getComponentStatusesJson(), Map.class);

            SystemMetrics metrics = new SystemMetrics();
            HealthStatus status = new HealthStatus(entity.getStatus(), entity.getMessage());

            Map<String, HealthStatus> componentHealthStatuses = componentStatuses.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> new HealthStatus(entry.getValue(), "")
                    ));

            return new Health(entity.getId(), status, metrics, componentHealthStatuses);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize health data", e);
        }
    }
}