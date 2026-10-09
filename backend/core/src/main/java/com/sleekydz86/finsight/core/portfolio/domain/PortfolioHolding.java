package com.sleekydz86.finsight.core.portfolio.domain;

public record PortfolioHolding(
        Long id,
        String name,
        PortfolioAssetKind kind,
        PortfolioAssetCategory category,
        long amount,
        Double profitRate,
        Long acquisitionAmount
) {
}
