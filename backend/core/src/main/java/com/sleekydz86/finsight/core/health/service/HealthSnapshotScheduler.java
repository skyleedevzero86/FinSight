package com.sleekydz86.finsight.core.health.service;

import com.sleekydz86.finsight.core.health.domain.Health;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthCommandUseCase;
import com.sleekydz86.finsight.core.health.domain.port.in.HealthQueryUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.health.snapshot.enabled", havingValue = "true", matchIfMissing = true)
public class HealthSnapshotScheduler {

    private final HealthQueryUseCase healthQueryUseCase;
    private final HealthCommandUseCase healthCommandUseCase;

    @Value("${app.health.snapshot.interval-ms:300000}")
    private long intervalMs;

    @Scheduled(fixedDelayString = "${app.health.snapshot.interval-ms:300000}", initialDelayString = "${app.health.snapshot.initial-delay-ms:60000}")
    public void collectSnapshot() {
        try {
            Health health = healthQueryUseCase.getCompleteHealth();
            healthCommandUseCase.saveHealthCheck(health);
            log.debug("헬스 스냅샷 저장 완료 - intervalMs={}", intervalMs);
        } catch (Exception e) {
            log.warn("헬스 스냅샷 저장 실패", e);
        }
    }
}
