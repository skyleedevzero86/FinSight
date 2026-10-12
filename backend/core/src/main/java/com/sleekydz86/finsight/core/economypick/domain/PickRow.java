package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record PickRow(
        String lane,
        String name,
        String symbol,
        String price,
        String changePct,
        String direction,
        String logo,
        String market,
        String volume,
        String detail,
        List<String> metrics) {
}
