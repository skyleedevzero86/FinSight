package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record EconomyPickChart(String symbol, String label, List<PriceBar> bars) {
}
