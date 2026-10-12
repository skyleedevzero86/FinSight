package com.sleekydz86.finsight.core.portfolio.domain;

import java.util.List;

public record PortfolioShareDetection(
        String source,
        String note,
        List<Chip> chips
) {
    public record Chip(String label, long count) {
    }
}
