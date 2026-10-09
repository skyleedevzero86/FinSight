package com.sleekydz86.finsight.core.portfolio.domain.port.in.dto;

public record PortfolioAssetCommand(
        String name,
        String kind,
        String category,
        long amount,
        Double profitRate,
        String custodian,
        Long acquisitionAmount,
        Double quantity,
        Long unitPrice,
        String memo
) {
}
