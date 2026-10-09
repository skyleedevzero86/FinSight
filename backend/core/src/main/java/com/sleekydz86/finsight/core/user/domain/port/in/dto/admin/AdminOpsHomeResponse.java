package com.sleekydz86.finsight.core.user.domain.port.in.dto.admin;

import java.util.Map;

public record AdminOpsHomeResponse(
        long totalUsers,
        long approvedUsers,
        long pendingUsers,
        long suspendedUsers,
        long withdrawnUsers,
        long totalBoards,
        long totalComments,
        long totalNews,
        Map<String, Object> healthSnapshot,
        Map<String, Object> metricsSnapshot,
        AdminStatsChartResponse signups,
        AdminStatsChartResponse logins,
        AdminStatsChartResponse content) {
}
