package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record EconomyPickSales(String symbol, String name, String note, List<Slice> slices) {

    public record Slice(String label, double percent, String sign) {
    }
}
