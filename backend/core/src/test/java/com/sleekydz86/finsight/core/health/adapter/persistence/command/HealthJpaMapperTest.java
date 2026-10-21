package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.vo.HealthStatus;
import com.sleekydz86.finsight.core.health.domain.vo.SystemMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("저장과 조회에서 지표와 구성 상태를 유지한다")
    void roundTripPreservesMetricsAndStructuredComponentStatuses() {
        // given
        LocalDateTime checkedAt = LocalDateTime.of(2026, 9, 4, 14, 15);
        Map<String, Object> jvm = new LinkedHashMap<>();

        // when
        jvm.put("threads", 18);
        jvm.put("memory", Map.of("used", 256, "max", 1024));

        // given
        Map<String, Object> system = Map.of("load", 0.75, "healthy", true);
        HealthStatus database = new HealthStatus(
                "UP", "reachable", Map.of("latencyMs", "12", "pool", "primary"));
        Health health = new Health(
                "snapshot-1",
                new HealthStatus("UP", "all systems operational"),
                new SystemMetrics(jvm, system),
                Map.of("database", database),
                checkedAt);

        // when
        HealthJpaEntity entity = mapper.toJpaEntity(health);
        Health restored = mapper.toDomain(entity);

        // then
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
    @DisplayName("예전 문자열 상태와 맵 상태를 읽는다")
    void readsLegacyStringStatusesAndLinkedHashMapStatuses() {
        // given
        HealthJpaEntity entity = entityWithJson(
                "{\"heap\":128}",
                "{\"cpu\":4}",
                "{\"legacy\":\"DOWN\",\"redis\":{\"status\":\"DEGRADED\",\"message\":\"slow\","
                        + "\"details\":{\"latency\":25,\"region\":null}}}");

        // when
        Health restored = mapper.toDomain(entity);

        // then
        assertThat(restored.getComponentStatuses().keySet()).containsExactly("legacy", "redis");
        assertThat(restored.getComponentStatuses().get("legacy").getStatus()).isEqualTo("DOWN");
        assertThat(restored.getComponentStatuses().get("legacy").getMessage()).isEmpty();
        assertThat(restored.getComponentStatuses().get("redis").getStatus()).isEqualTo("DEGRADED");
        assertThat(restored.getComponentStatuses().get("redis").getDetails())
                .containsEntry("latency", "25")
                .containsEntry("region", "");
    }

    @Test
    @DisplayName("없거나 예상 밖인 상태는 정해진 값으로 대체한다")
    void missingAndUnexpectedStatusValuesReceiveStableFallbacks() {
        // given
        HealthJpaEntity entity = entityWithJson(
                "{}", "{}",
                "{\"missing\":null,\"blank\":{\"status\":\"  \",\"message\":null},\"numeric\":503}");

        // when
        Health restored = mapper.toDomain(entity);

        // then
        assertThat(restored.getComponentStatuses().get("missing").getStatus()).isEqualTo("UNKNOWN");
        assertThat(restored.getComponentStatuses().get("blank").getStatus()).isEqualTo("UNKNOWN");
        assertThat(restored.getComponentStatuses().get("blank").getMessage()).isEmpty();
        assertThat(restored.getComponentStatuses().get("numeric").getStatus()).isEqualTo("503");
    }

    @Test
    @DisplayName("없는 지표는 빈 객체로 저장하고 빈 JSON은 빈 맵으로 읽는다")
    void nullMetricsSerializeAsEmptyObjectsAndBlankJsonDeserializesAsEmptyMaps() {
        // given
        Health health = new Health(
                "empty",
                new HealthStatus("UP", ""),
                null,
                null,
                LocalDateTime.of(2026, 9, 4, 14, 30));

        // when
        HealthJpaEntity serialized = mapper.toJpaEntity(health);

        // then
        assertThat(serialized.getJvmMetricsJson()).isEqualTo("{}");
        assertThat(serialized.getSystemMetricsJson()).isEqualTo("{}");
        assertThat(serialized.getComponentStatusesJson()).isEqualTo("{}");

        // given
        HealthJpaEntity blank = entityWithJson(null, "   ", "");

        // when
        Health restored = mapper.toDomain(blank);

        // then
        assertThat(restored.getMetrics().getJvmMetrics()).isEmpty();
        assertThat(restored.getMetrics().getSystemMetrics()).isEmpty();
        assertThat(restored.getComponentStatuses()).isEmpty();
    }

    @Test
    @DisplayName("잘못된 JSON은 역직렬화 실패로 알린다")
    void invalidJsonIsReportedAsDeserializationFailure() {
        // given
        HealthJpaEntity entity = entityWithJson("{not-json", "{}", "{}");

        // when

        // then
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
