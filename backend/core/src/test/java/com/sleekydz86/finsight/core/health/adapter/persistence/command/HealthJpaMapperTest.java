package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthJpaMapperTest {

    private HealthJpaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new HealthJpaMapper(new ObjectMapper());
    }

    @Test
    void roundTripPreservesMetricsAndStructuredComponentStatuses() {
        LocalDateTime checkedAt = LocalDateTime.of(2026, 9, 4, 14, 15);
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("threads", 18);
        jvm.put("memory", Map.of("used", 256, "max", 1024));
        Map<String, Object> system = Map.of("load", 0.75, "healthy", true);
        HealthStatus database = new HealthStatus(
                "UP", "reachable", Map.of("latencyMs", "12", "pool", "primary"));
        Health health = new Health(
                "snapshot-1",
                new HealthStatus("UP", "all systems operational"),
                new SystemMetrics(jvm, system),
                Map.of("database", database),
                checkedAt);

        HealthJpaEntity entity = mapper.toJpaEntity(health);
        Health restored = mapper.toDomain(entity);

        assertThat(entity.getJvmMetricsJson()).contains("threads", "memory");
        assertThat(entity.getComponentStatusesJson()).contains("latencyMs", "reachable");
        assertThat(restored.getId()).isEqualTo("snapshot-1");
        assertThat(restored.getStatus().getStatus()).isEqualTo("UP");
        assertThat(restored.getStatus().getMessage()).isEqualTo("all systems operational");
        assertThat(restored.getCheckedAt()).isEqualTo(checkedAt);
        assertThat(restored.getMetrics().getJvmMetrics()).containsEntry("threads", 18);
        assertThat(restored.getMetrics().getJvmMetrics().get("memory")).isEqualTo(Map.of("used", 256, "max", 1024));
        assertThat(restored.getMetrics().getSystemMetrics()).containsEntry("healthy", true);
        assertThat(restored.getComponentStatuses().get("database").getStatus()).isEqualTo("UP");
        assertThat(restored.getComponentStatuses().get("database").getMessage()).isEqualTo("reachable");
        assertThat(restored.getComponentStatuses().get("database").getDetails())
                .containsExactlyInAnyOrderEntriesOf(Map.of("latencyMs", "12", "pool", "primary"));
    }

    @Test
    void readsLegacyStringStatusesAndLinkedHashMapStatuses() {
        HealthJpaEntity entity = entityWithJson(
                "{\"heap\":128}",
                "{\"cpu\":4}",
                "{\"legacy\":\"DOWN\",\"redis\":{\"status\":\"DEGRADED\",\"message\":\"slow\","
                        + "\"details\":{\"latency\":25,\"region\":null}}}");

        Health restored = mapper.toDomain(entity);

        assertThat(restored.getComponentStatuses().keySet()).containsExactly("legacy", "redis");
        assertThat(restored.getComponentStatuses().get("legacy").getStatus()).isEqualTo("DOWN");
        assertThat(restored.getComponentStatuses().get("legacy").getMessage()).isEmpty();
        assertThat(restored.getComponentStatuses().get("redis").getStatus()).isEqualTo("DEGRADED");
        assertThat(restored.getComponentStatuses().get("redis").getDetails())
                .containsEntry("latency", "25")
                .containsEntry("region", "");
    }

    @Test
    void missingAndUnexpectedStatusValuesReceiveStableFallbacks() {
        HealthJpaEntity entity = entityWithJson(
                "{}", "{}",
                "{\"missing\":null,\"blank\":{\"status\":\"  \",\"message\":null},\"numeric\":503}");

        Health restored = mapper.toDomain(entity);

        assertThat(restored.getComponentStatuses().get("missing").getStatus()).isEqualTo("UNKNOWN");
        assertThat(restored.getComponentStatuses().get("blank").getStatus()).isEqualTo("UNKNOWN");
        assertThat(restored.getComponentStatuses().get("blank").getMessage()).isEmpty();
        assertThat(restored.getComponentStatuses().get("numeric").getStatus()).isEqualTo("503");
    }

    @Test
    void nullMetricsSerializeAsEmptyObjectsAndBlankJsonDeserializesAsEmptyMaps() {
        Health health = new Health(
                "empty",
                new HealthStatus("UP", ""),
                null,
                null,
                LocalDateTime.of(2026, 9, 4, 14, 30));

        HealthJpaEntity serialized = mapper.toJpaEntity(health);

        assertThat(serialized.getJvmMetricsJson()).isEqualTo("{}");
        assertThat(serialized.getSystemMetricsJson()).isEqualTo("{}");
        assertThat(serialized.getComponentStatusesJson()).isEqualTo("{}");

        HealthJpaEntity blank = entityWithJson(null, "   ", "");
        Health restored = mapper.toDomain(blank);
        assertThat(restored.getMetrics().getJvmMetrics()).isEmpty();
        assertThat(restored.getMetrics().getSystemMetrics()).isEmpty();
        assertThat(restored.getComponentStatuses()).isEmpty();
    }

    @Test
    void invalidJsonIsReportedAsDeserializationFailure() {
        HealthJpaEntity entity = entityWithJson("{not-json", "{}", "{}");

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("헬스 데이터 역직렬화에 실패했습니다")
                .hasCauseInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }

    private HealthJpaEntity entityWithJson(String jvm, String system, String components) {
        HealthJpaEntity entity = new HealthJpaEntity();
        entity.setId("snapshot");
        entity.setStatus("UP");
        entity.setMessage("ok");
        entity.setCheckedAt(LocalDateTime.of(2026, 9, 4, 12, 0));
        entity.setJvmMetricsJson(jvm);
        entity.setSystemMetricsJson(system);
        entity.setComponentStatusesJson(components);
        return entity;
    }
}
