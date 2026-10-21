package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record SpaceXSnapshot(List<Stock> stocks) {

    public record Stock(
            String symbol,
            String name,
            String group,
            String price,
            String changePct,
            String signal,
            double score,
            Double rsi,
            Double williamsR,
            String low52w,
            String high52w,
            String volume) {
    }
}
