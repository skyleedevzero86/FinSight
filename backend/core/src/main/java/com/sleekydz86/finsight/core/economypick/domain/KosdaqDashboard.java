package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record KosdaqDashboard(
        int companyCount,
        int highRiskCount,
        int nameChanges,
        int holderChanges,
        int lawsuits,
        int cbIssuances,
        int capitalChanges,
        List<Signal> signals) {

    public record Signal(
            String name,
            String symbol,
            String price,
            String changePct,
            int risk,
            boolean highRisk,
            String rules,
            String detail) {
    }
}
