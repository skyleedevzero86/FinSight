package com.sleekydz86.finsight.core.notification.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "SMS 발송 통계")
public record SmsStatsResponse(
        @Schema(description = "전체 발송 성공") long totalSent,
        @Schema(description = "전체 실패") long totalFailed,
        @Schema(description = "전체 스킵") long totalSkipped,
        @Schema(description = "최근 7일 성공") long sentLast7Days,
        @Schema(description = "최근 7일 실패") long failedLast7Days,
        @Schema(description = "용도별 건수") Map<String, Long> byPurpose,
        @Schema(description = "일별 상태 집계") List<DailyPoint> daily
) {
    @Schema(description = "일별 집계 포인트")
    public record DailyPoint(
            String date,
            long sent,
            long failed,
            long skipped
    ) {
    }
}
