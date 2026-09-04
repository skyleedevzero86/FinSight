package com.sleekydz86.finsight.core.user.domain.port.in.dto.admin;

import java.time.LocalDate;
import java.util.List;

public record AdminStatsChartResponse(
        String chartKey,
        String title,
        String unit,
        List<AdminStatsNamedSeries> series,
        LocalDate from,
        LocalDate to) {
}
