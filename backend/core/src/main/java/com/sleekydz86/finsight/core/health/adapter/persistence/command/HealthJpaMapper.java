package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HealthJpaMapper {

    private static final TypeReference<Map<String, Object>> OBJECT_MAP_TYPE = new TypeReference<>() {
    };

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
            entity.setJvmMetricsJson(objectMapper.writeValueAsString(
                    nullToEmpty(health.getMetrics() != null ? health.getMetrics().getJvmMetrics() : null)));
            entity.setSystemMetricsJson(objectMapper.writeValueAsString(
                    nullToEmpty(health.getMetrics() != null ? health.getMetrics().getSystemMetrics() : null)));
            entity.setComponentStatusesJson(objectMapper.writeValueAsString(
                    toComponentStatusPayload(health.getComponentStatuses())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("헬스 데이터 직렬화에 실패했습니다", e);
        }

        return entity;
    }

    public Health toDomain(HealthJpaEntity entity) {
        try {
            Map<String, Object> jvmMetrics = readObjectMap(entity.getJvmMetricsJson());
            Map<String, Object> systemMetrics = readObjectMap(entity.getSystemMetricsJson());
            Map<String, HealthStatus> componentHealthStatuses = readComponentStatuses(entity.getComponentStatusesJson());

            SystemMetrics metrics = new SystemMetrics(jvmMetrics, systemMetrics);
            HealthStatus status = new HealthStatus(entity.getStatus(), entity.getMessage());

            return new Health(entity.getId(), status, metrics, componentHealthStatuses, entity.getCheckedAt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("헬스 데이터 역직렬화에 실패했습니다", e);
        }
    }

    private Map<String, Object> readObjectMap(String json) throws JsonProcessingException {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, Object> parsed = objectMapper.readValue(json, OBJECT_MAP_TYPE);
        return parsed != null ? parsed : Collections.emptyMap();
    }

    private Map<String, HealthStatus> readComponentStatuses(String json) throws JsonProcessingException {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, Object> raw = objectMapper.readValue(json, OBJECT_MAP_TYPE);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, HealthStatus> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), toHealthStatus(entry.getValue()));
        }
        return result;
    }

    private Map<String, Object> toComponentStatusPayload(Map<String, HealthStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        for (Map.Entry<String, HealthStatus> entry : statuses.entrySet()) {
            HealthStatus healthStatus = entry.getValue();
            if (healthStatus == null) {
                payload.put(entry.getKey(), null);
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("status", healthStatus.getStatus());
            node.put("message", healthStatus.getMessage());
            node.put("details", healthStatus.getDetails() != null
                    ? healthStatus.getDetails()
                    : Collections.emptyMap());
            payload.put(entry.getKey(), node);
        }
        return payload;
    }

    private HealthStatus toHealthStatus(Object value) {
        if (value == null) {
            return new HealthStatus("UNKNOWN", "");
        }
        if (value instanceof HealthStatus healthStatus) {
            return healthStatus;
        }
        if (value instanceof String status) {
            return new HealthStatus(status, "");
        }
        if (value instanceof Map<?, ?> map) {
            String status = asString(map.get("status"), "UNKNOWN");
            String message = asString(map.get("message"), "");
            Map<String, String> details = new LinkedHashMap<>();
            Object detailsObj = map.get("details");
            if (detailsObj instanceof Map<?, ?> detailsMap) {
                for (Map.Entry<?, ?> detail : detailsMap.entrySet()) {
                    if (detail.getKey() == null) {
                        continue;
                    }
                    details.put(String.valueOf(detail.getKey()),
                            detail.getValue() == null ? "" : String.valueOf(detail.getValue()));
                }
            }
            return new HealthStatus(status, message, details);
        }
        return new HealthStatus(String.valueOf(value), "");
    }

    private String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private <K, V> Map<K, V> nullToEmpty(Map<K, V> map) {
        return map != null ? map : Collections.emptyMap();
    }
}
