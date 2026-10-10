package com.sleekydz86.finsight.core.portfolio.domain;

import com.sleekydz86.finsight.core.portfolio.domain.PortfolioSummary.InsightLine;

import java.util.List;

public record PortfolioDiagnosis(
        List<InsightLine> insights,
        String advice,
        String source
) {
    public static final String LLAMA2 = "LLAMA2";
    public static final String RULE = "RULE";

    public PortfolioDiagnosis {
        insights = insights == null ? List.of() : List.copyOf(insights);
        advice = advice == null ? "" : advice;
        source = source == null || source.isBlank() ? RULE : source;
    }

    public static PortfolioDiagnosis fromRules(PortfolioSummary summary) {
        return new PortfolioDiagnosis(summary.insights(), summary.advice(), RULE);
    }
}
