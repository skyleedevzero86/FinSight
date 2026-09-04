package com.sleekydz86.finsight.core.user.domain.port.in.dto.admin;

import java.util.List;

public record AdminStatsNamedSeries(String name, String label, List<AdminStatsSeriesPoint> points) {
}
