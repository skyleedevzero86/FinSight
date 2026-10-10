package com.sleekydz86.finsight.core.portfolio.domain;

import java.util.List;

public record PortfolioShareView(
        boolean showNetWorth,
        boolean showMonthRate,
        boolean showAllocation,
        boolean showGoal,
        boolean showExactNames,
        boolean showDebt,
        boolean showPrincipal,
        boolean showProfit,
        String amountMode,
        String visibility,
        String message,
        String goalLabel,
        String amountLabel,
        int progressPercent,
        String monthRateLabel,
        String recordMonth,
        String totalAssetsLabel,
        String totalLiabilitiesLabel,
        String netWorthLabel,
        List<AssetLine> assets,
        List<HistoryLine> history
) {
    public record AssetLine(
            String category,
            String name,
            String amountLabel,
            String acquisitionLabel,
            String quantityLabel,
            String unitPriceLabel,
            String profitLabel
    ) {
    }

    public record HistoryLine(
            String month,
            String netWorthLabel,
            String deltaLabel,
            String statusLabel
    ) {
    }
}
