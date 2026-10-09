package com.sleekydz86.finsight.core.portfolio.domain;

import java.util.List;

public record PortfolioSummary(
        long totalAssets,
        long totalLiabilities,
        long netWorth,
        long goalAmount,
        int goalPercentTenths,
        Long monthDelta,
        Long yearDelta,
        Long averageMonthlyGain,
        String expectedGoalLabel,
        Integer debtPercentTenths,
        List<AllocationSlice> allocation,
        List<TrendBar> trend,
        List<AssetRow> assets,
        List<InsightLine> insights,
        String advice,
        boolean recordedThisMonth,
        List<HistoryRow> history
) {
    public static final long GOAL_AMOUNT = 100_000_000L;

    public record AllocationSlice(String category, String label, long amount, int percentTenths) {
    }

    public record TrendBar(String label, long netWorth, int heightPercent) {
    }

    public record AssetRow(
            Long id,
            String name,
            String kind,
            String category,
            String categoryLabel,
            long amount,
            Double profitRate,
            int sharePercentTenths
    ) {
    }

    public record InsightLine(String tone, String text) {
    }

    public record HistoryRow(String yearMonth, long netWorth, Long delta, String status) {
    }
}
