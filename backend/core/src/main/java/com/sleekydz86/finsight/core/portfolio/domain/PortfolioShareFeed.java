package com.sleekydz86.finsight.core.portfolio.domain;

import java.util.List;

public record PortfolioShareFeed(
        List<PortfolioShareCard> cards,
        List<PortfolioShareGoal> goals,
        boolean hasNext
) {
    public record PortfolioShareCard(
            long id,
            String authorName,
            String message,
            String goalLabel,
            String amountLabel,
            int progressPercent,
            String monthRateLabel,
            String goalRateLabel,
            boolean showAsset,
            boolean showDebt,
            int cheerCount,
            String sharedAt
    ) {
    }

    public record PortfolioShareGoal(String label, long participantCount) {
    }
}
