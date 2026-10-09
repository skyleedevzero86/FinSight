package com.sleekydz86.finsight.core.portfolio.domain.port.in.dto;

public record PortfolioShareCommand(
        String message,
        String visibility,
        String amountMode,
        boolean showNetWorth,
        boolean showMonthRate,
        boolean showAllocation,
        boolean showGoal,
        boolean showDebt
) {
}
