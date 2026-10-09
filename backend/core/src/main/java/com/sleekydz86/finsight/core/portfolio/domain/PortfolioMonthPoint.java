package com.sleekydz86.finsight.core.portfolio.domain;

import java.time.YearMonth;

public record PortfolioMonthPoint(
        YearMonth month,
        long netWorth,
        long liabilities
) {
}
